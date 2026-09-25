package app.dao;

import app.entities.Assignment;
import app.entities.AssignmentHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AssignmentDAO {

    private final EntityManagerFactory emf;
    private final AssignmentHistoryDAO historyDAO;

    // Overloaded constructor so instantiation with just `new AssignmentDAO(emf)` works cleanly
    public AssignmentDAO(EntityManagerFactory emf) {
        this(emf, new AssignmentHistoryDAO(emf));
    }

    public AssignmentDAO(EntityManagerFactory emf, AssignmentHistoryDAO historyDAO) {
        this.emf = emf;
        this.historyDAO = historyDAO;
    }

    public Assignment create(Assignment assignment, Long createdByUserId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(assignment);

            // Audit Log (7-param constructor)
            AssignmentHistory historyRecord = new AssignmentHistory(
                    assignment.getId(),
                    "CREATE",
                    null, // previousEmployeeId
                    assignment.getAssignedEmployeeId(), // newEmployeeId
                    String.valueOf(createdByUserId),
                    Instant.now(),
                    "Created assignment: " + assignment.getName()
            );
            historyDAO.log(em, historyRecord);

            em.getTransaction().commit();
            return assignment;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Assignment getById(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Assignment.class, id);
        } finally {
            em.close();
        }
    }

    public List<Assignment> getAll(Long tenantId, boolean activeOnly) {
        EntityManager em = emf.createEntityManager();
        try {
            String jpql = "SELECT a FROM Assignment a WHERE a.tenantId = :tenantId";
            if (activeOnly) {
                jpql += " AND a.isActive = true";
            }
            TypedQuery<Assignment> query = em.createQuery(jpql, Assignment.class);
            query.setParameter("tenantId", tenantId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public Assignment update(Assignment updatedAssignment, Long changedByUserId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Assignment existing = em.find(Assignment.class, updatedAssignment.getId());
            if (existing == null) {
                throw new IllegalArgumentException("Assignment not found with ID: " + updatedAssignment.getId());
            }

            Long oldEmployeeId = existing.getAssignedEmployeeId();
            List<String> diffs = new ArrayList<>();

            if (!Objects.equals(existing.getName(), updatedAssignment.getName())) {
                diffs.add(String.format("Name: '%s' -> '%s'", existing.getName(), updatedAssignment.getName()));
                existing.setName(updatedAssignment.getName());
            }

            if (!Objects.equals(existing.getAddress(), updatedAssignment.getAddress())) {
                diffs.add(String.format("Address: '%s' -> '%s'", existing.getAddress(), updatedAssignment.getAddress()));
                existing.setAddress(updatedAssignment.getAddress());
            }

            if (!Objects.equals(existing.getEstimatedMinutes(), updatedAssignment.getEstimatedMinutes())) {
                diffs.add(String.format("Estimated Minutes: %s -> %s", existing.getEstimatedMinutes(), updatedAssignment.getEstimatedMinutes()));
                existing.setEstimatedMinutes(updatedAssignment.getEstimatedMinutes());
            }

            if (!Objects.equals(existing.getCost(), updatedAssignment.getCost())) {
                diffs.add(String.format("Cost: %s -> %s", existing.getCost(), updatedAssignment.getCost()));
                existing.setCost(updatedAssignment.getCost());
            }

            if (existing.isActive() != updatedAssignment.isActive()) {
                diffs.add(String.format("Active: %b -> %b", existing.isActive(), updatedAssignment.isActive()));
                existing.setActive(updatedAssignment.isActive());
            }

            if (existing.isFlagged() != updatedAssignment.isFlagged()) {
                diffs.add(String.format("Flagged: %b -> %b", existing.isFlagged(), updatedAssignment.isFlagged()));
                existing.setFlagged(updatedAssignment.isFlagged());
            }

            if (!Objects.equals(existing.getAssignedEmployeeId(), updatedAssignment.getAssignedEmployeeId())) {
                diffs.add(String.format("Assigned Employee ID: %s -> %s", existing.getAssignedEmployeeId(), updatedAssignment.getAssignedEmployeeId()));
                existing.setAssignedEmployeeId(updatedAssignment.getAssignedEmployeeId());
            }

            Assignment merged = em.merge(existing);

            if (!diffs.isEmpty()) {
                String summary = String.join("; ", diffs);

                AssignmentHistory historyRecord = new AssignmentHistory(
                        merged.getId(),
                        "UPDATE",
                        oldEmployeeId,
                        merged.getAssignedEmployeeId(),
                        String.valueOf(changedByUserId),
                        Instant.now(),
                        summary
                );

                historyDAO.log(em, historyRecord);
            }

            em.getTransaction().commit();
            return merged;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Assignment reassign(Long assignmentId, Long newEmployeeId, Long changedByUserId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Assignment existing = em.find(Assignment.class, assignmentId);
            if (existing == null) {
                throw new IllegalArgumentException("Assignment not found with ID: " + assignmentId);
            }

            Long oldEmployeeId = existing.getAssignedEmployeeId();
            existing.setAssignedEmployeeId(newEmployeeId);
            Assignment merged = em.merge(existing);

            String details = String.format("Reassigned from Employee ID %s to ID %s", oldEmployeeId, newEmployeeId);

            AssignmentHistory historyRecord = new AssignmentHistory(
                    merged.getId(),
                    "REASSIGN",
                    oldEmployeeId,
                    newEmployeeId,
                    String.valueOf(changedByUserId),
                    Instant.now(),
                    details
            );

            historyDAO.log(em, historyRecord);

            em.getTransaction().commit();
            return merged;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public void delete(Long id, Long deletedByUserId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Assignment existing = em.find(Assignment.class, id);
            if (existing != null) {
                AssignmentHistory historyRecord = new AssignmentHistory(
                        existing.getId(),
                        "DELETE",
                        existing.getAssignedEmployeeId(),
                        null,
                        String.valueOf(deletedByUserId),
                        Instant.now(),
                        "Deleted assignment: " + existing.getName()
                );

                historyDAO.log(em, historyRecord);
                em.remove(existing);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public Assignment cancel(Long assignmentId) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Assignment existing = em.find(Assignment.class, assignmentId);
            if (existing == null) {
                throw new IllegalArgumentException("Assignment not found with ID: " + assignmentId);
            }

            existing.setActive(false);
            existing.setFlagged(false);

            Assignment merged = em.merge(existing);
            em.getTransaction().commit();
            return merged;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
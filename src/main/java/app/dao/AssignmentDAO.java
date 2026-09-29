package app.dao;

import app.entities.Assignment;
import app.entities.AssignmentAuditHistory;
import app.entities.AssignmentStateHistory;
import app.exceptions.AssignmentInUseException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.sql.SQLException;
import java.util.List;

public class AssignmentDAO {

    // PostgreSQL SQLSTATE for foreign_key_violation
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final EntityManagerFactory emf;

    public AssignmentDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<Assignment> getAll(Long tenantId, boolean activeOnly) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT a FROM Assignment a WHERE a.tenantId = :tenantId"
                    + (activeOnly ? " AND a.isActive = true" : "")
                    + " ORDER BY LOWER(a.name), a.id";
            return em.createQuery(jpql, Assignment.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public Assignment getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Assignment.class, id);
        }
    }

    /** Assignments of the tenant whose name matches, ignoring case. */
    public List<Assignment> findByName(Long tenantId, String name) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT a FROM Assignment a WHERE a.tenantId = :tenantId AND LOWER(a.name) = LOWER(:name)",
                            Assignment.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("name", name)
                    .getResultList();
        }
    }

    public Assignment create(Assignment assignment) {
        return create(assignment, null);
    }

    public Assignment create(Assignment assignment, AssignmentStateHistory stateHistory) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(assignment);
            if (stateHistory != null) {
                stateHistory.setAssignmentId(assignment.getId());
                em.persist(stateHistory);
            }
            tx.commit();
            return assignment;
        }
    }

    public Assignment update(Assignment assignment) {
        return update(assignment, (AssignmentStateHistory) null);
    }

    public Assignment update(Assignment assignment, AssignmentStateHistory stateHistory) {
        return update(assignment, stateHistory, null);
    }

    public Assignment update(Assignment assignment, AssignmentAuditHistory auditHistory) {
        return update(assignment, null, auditHistory);
    }

    public Assignment update(Assignment assignment, AssignmentStateHistory stateHistory, AssignmentAuditHistory auditHistory) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Assignment updated = em.merge(assignment);
            if (stateHistory != null) {
                em.persist(stateHistory);
            }
            if (auditHistory != null) {
                auditHistory.setAssignmentId(updated.getId());
                em.persist(auditHistory);
            }
            tx.commit();
            return updated;
        }
    }

    public List<AssignmentStateHistory> getStateHistory(Long assignmentId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT h FROM AssignmentStateHistory h WHERE h.assignmentId = :assignmentId ORDER BY h.changedAt DESC, h.id DESC",
                            AssignmentStateHistory.class)
                    .setParameter("assignmentId", assignmentId)
                    .getResultList();
        }
    }

    public List<AssignmentAuditHistory> getAuditHistory(Long assignmentId, String auditType) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT h FROM AssignmentAuditHistory h WHERE h.assignmentId = :assignmentId"
                    + (auditType == null ? "" : " AND h.auditType = :auditType")
                    + " ORDER BY h.createdAt DESC, h.id DESC";
            var query = em.createQuery(jpql, AssignmentAuditHistory.class)
                    .setParameter("assignmentId", assignmentId);
            if (auditType != null) {
                query.setParameter("auditType", auditType);
            }
            return query.getResultList();
        }
    }

    public List<Assignment> findOverlappingAssignments(Long tenantId, Long assignedEmployeeId,
                                                       java.time.LocalDateTime startTime,
                                                       java.time.LocalDateTime estimatedEndTime,
                                                       Long ownId) {
        if (tenantId == null || assignedEmployeeId == null || startTime == null || estimatedEndTime == null) {
            return List.of();
        }
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT a FROM Assignment a
                            WHERE a.tenantId = :tenantId
                              AND a.assignedEmployeeId = :assignedEmployeeId
                              AND a.isActive = true
                              AND (:ownId IS NULL OR a.id <> :ownId)
                              AND a.startTime IS NOT NULL
                              AND a.estimatedEndTime IS NOT NULL
                              AND a.startTime < :estimatedEndTime
                              AND a.estimatedEndTime > :startTime
                            ORDER BY a.startTime, a.id
                            """, Assignment.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("assignedEmployeeId", assignedEmployeeId)
                    .setParameter("ownId", ownId)
                    .setParameter("startTime", startTime)
                    .setParameter("estimatedEndTime", estimatedEndTime)
                    .getResultList();
        }
    }

    public List<Assignment> getVisibleForCategory(Long tenantId, String primaryCategory, boolean activeOnly) {
        if (tenantId == null || primaryCategory == null || primaryCategory.isBlank()) {
            return List.of();
        }
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = """
                    SELECT a FROM Assignment a
                    WHERE a.tenantId = :tenantId
                      AND a.assignedEmployeeId IN (
                        SELECT u.id FROM User u
                        WHERE u.tenantId = :tenantId
                          AND LOWER(u.primaryCategory) = LOWER(:primaryCategory)
                          AND u.isActive = true
                      )
                    """
                    + (activeOnly ? " AND a.isActive = true" : "")
                    + " ORDER BY a.startTime, LOWER(a.name), a.id";
            return em.createQuery(jpql, Assignment.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("primaryCategory", primaryCategory)
                    .getResultList();
        }
    }

    /**
     * @throws AssignmentInUseException when a foreign key from another table still points at the assignment
     */
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Assignment entity = em.find(Assignment.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            tx.commit();
        } catch (RuntimeException e) {
            if (isForeignKeyViolation(e)) {
                throw new AssignmentInUseException(e);
            }
            throw e;
        }
    }

    private static boolean isForeignKeyViolation(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && FOREIGN_KEY_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}

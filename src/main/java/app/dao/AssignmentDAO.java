package app.dao;

import app.entities.Assignment;
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

    public List<Assignment> getByAssignedEmployeePrimaryCategory(Long tenantId, Long categoryId, boolean activeOnly) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT a FROM Assignment a, User u "
                    + "WHERE a.tenantId = :tenantId AND a.assignedEmployeeId = u.id "
                    + "AND u.primaryCategory.id = :categoryId"
                    + (activeOnly ? " AND a.isActive = true" : "")
                    + " ORDER BY LOWER(a.name), a.id";
            return em.createQuery(jpql, Assignment.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("categoryId", categoryId)
                    .getResultList();
        }
    }

    public Assignment create(Assignment assignment) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(assignment);
            tx.commit();
            return assignment;
        }
    }

    public Assignment update(Assignment assignment) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Assignment updated = em.merge(assignment);
            tx.commit();
            return updated;
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

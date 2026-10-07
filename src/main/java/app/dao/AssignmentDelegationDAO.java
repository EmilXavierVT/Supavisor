package app.dao;

import app.entities.Assignment;
import app.entities.AssignmentDelegation;
import app.entities.AssignmentHistory;
import app.entities.User;
import app.exceptions.AssignmentAlreadyDelegatedException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

public class AssignmentDelegationDAO {

    private static final String UNIQUE_VIOLATION = "23505";

    private final EntityManagerFactory emf;

    public AssignmentDelegationDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<AssignmentDelegation> findByAssignmentId(Long assignmentId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT d FROM AssignmentDelegation d
                            JOIN FETCH d.employee
                            WHERE d.assignment.id = :assignmentId
                            ORDER BY d.delegatedAt, d.id
                            """, AssignmentDelegation.class)
                    .setParameter("assignmentId", assignmentId)
                    .getResultList();
        }
    }

    public List<AssignmentDelegation> findByTenantId(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT d FROM AssignmentDelegation d
                            JOIN FETCH d.employee
                            WHERE d.assignment.tenantId = :tenantId
                            ORDER BY d.delegatedAt, d.id
                            """, AssignmentDelegation.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public boolean isDelegated(Long assignmentId, Long employeeId) {
        if (assignmentId == null || employeeId == null) {
            return false;
        }
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT COUNT(d) FROM AssignmentDelegation d
                            WHERE d.assignment.id = :assignmentId AND d.employee.id = :employeeId
                            """, Long.class)
                    .setParameter("assignmentId", assignmentId)
                    .setParameter("employeeId", employeeId)
                    .getSingleResult() > 0;
        }
    }

    /** Saves the delegation and its history entry together, so a failure leaves nothing behind. */
    public AssignmentDelegation create(Long assignmentId, Long employeeId, Long delegatedByUserId,
                                       String delegatedBy, Instant delegatedAt, AssignmentHistory history) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                AssignmentDelegation delegation = AssignmentDelegation.builder()
                        .assignment(em.getReference(Assignment.class, assignmentId))
                        .employee(em.find(User.class, employeeId))
                        .delegatedByUserId(delegatedByUserId)
                        .delegatedBy(delegatedBy)
                        .delegatedAt(delegatedAt)
                        .build();
                em.persist(delegation);
                if (history != null) {
                    em.persist(history);
                }
                tx.commit();
                return delegation;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                if (isUniqueViolation(e)) {
                    throw new AssignmentAlreadyDelegatedException(e);
                }
                throw e;
            }
        }
    }

    /** Removes the delegation and saves its history entry together. Returns false when nothing was delegated. */
    public boolean delete(Long assignmentId, Long employeeId, AssignmentHistory history) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                int removed = em.createQuery("""
                                DELETE FROM AssignmentDelegation d
                                WHERE d.assignment.id = :assignmentId AND d.employee.id = :employeeId
                                """)
                        .setParameter("assignmentId", assignmentId)
                        .setParameter("employeeId", employeeId)
                        .executeUpdate();
                if (removed == 0) {
                    tx.rollback();
                    return false;
                }
                if (history != null) {
                    em.persist(history);
                }
                tx.commit();
                return true;
            } catch (RuntimeException e) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
        }
    }

    private static boolean isUniqueViolation(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}

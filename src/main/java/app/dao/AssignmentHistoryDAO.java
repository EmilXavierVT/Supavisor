package app.dao;

import app.entities.AssignmentHistory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class AssignmentHistoryDAO {

    private final EntityManagerFactory emf;

    public AssignmentHistoryDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public void log(EntityManager em, AssignmentHistory history) {
        if (history != null) {
            em.persist(history);
        }
    }

    public AssignmentHistory create(AssignmentHistory history) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(history);
            em.getTransaction().commit();
            return history;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public List<AssignmentHistory> findByAssignmentId(Long assignmentId) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<AssignmentHistory> query = em.createQuery(
                    "SELECT h FROM AssignmentHistory h WHERE h.assignmentId = :assignmentId ORDER BY h.changedAt DESC",
                    AssignmentHistory.class
            );
            query.setParameter("assignmentId", assignmentId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
}
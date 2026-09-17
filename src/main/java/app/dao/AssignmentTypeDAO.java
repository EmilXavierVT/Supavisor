package app.dao;

import app.entities.AssignmentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class AssignmentTypeDAO {

    private final EntityManagerFactory emf;

    public AssignmentTypeDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<AssignmentType> getAll(boolean activeOnly) {
        try (EntityManager em = emf.createEntityManager()) {
            if (activeOnly) {
                return em.createQuery("SELECT a FROM AssignmentType a WHERE a.isActive = true", AssignmentType.class)
                        .getResultList();
            }
            return em.createQuery("SELECT a FROM AssignmentType a", AssignmentType.class)
                    .getResultList();
        }
    }

    public AssignmentType getById(Integer id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(AssignmentType.class, id);
        }
    }

    public AssignmentType create(AssignmentType assignmentType) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(assignmentType);
            tx.commit();
            return assignmentType;
        }
    }

    public AssignmentType update(AssignmentType assignmentType) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            AssignmentType updated = em.merge(assignmentType);
            tx.commit();
            return updated;
        }
    }

    public void delete(Integer id) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            AssignmentType entity = em.find(AssignmentType.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            tx.commit();
        }
    }
}
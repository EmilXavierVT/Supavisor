package app.dao;

import app.entities.Project;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class ProjectDAO {

    private final EntityManagerFactory emf;

    public ProjectDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<Project> getAll(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT p FROM Project p WHERE p.tenantId = :tenantId ORDER BY LOWER(p.name), p.id",
                            Project.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public Project getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Project.class, id);
        }
    }

    public List<Project> findByName(Long tenantId, String name) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT p FROM Project p WHERE p.tenantId = :tenantId AND LOWER(p.name) = LOWER(:name)",
                            Project.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("name", name)
                    .getResultList();
        }
    }

    public Project create(Project project) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            em.persist(project);
            transaction.commit();
            return project;
        }
    }

    public Project update(Project project) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Project updated = em.merge(project);
            tx.commit();
            return updated;
        }
    }

    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Project entity = em.find(Project.class, id);
            if (entity != null) {
                em.remove(entity);
            }
            tx.commit();
        }
    }
}

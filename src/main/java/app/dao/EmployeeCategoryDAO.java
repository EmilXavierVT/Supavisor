package app.dao;

import app.entities.EmployeeCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class EmployeeCategoryDAO {
    private final EntityManagerFactory emf;

    public EmployeeCategoryDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<EmployeeCategory> getAll(Long tenantId, boolean activeOnly) {
        try (EntityManager em = emf.createEntityManager()) {
            String jpql = "SELECT c FROM EmployeeCategory c WHERE c.tenantId = :tenantId"
                    + (activeOnly ? " AND c.active = true" : "")
                    + " ORDER BY LOWER(c.name), c.id";
            return em.createQuery(jpql, EmployeeCategory.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public EmployeeCategory getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(EmployeeCategory.class, id);
        }
    }

    public EmployeeCategory create(EmployeeCategory category) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(category);
            em.getTransaction().commit();
            return category;
        }
    }

    public EmployeeCategory update(EmployeeCategory category) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            EmployeeCategory saved = em.merge(category);
            em.getTransaction().commit();
            return saved;
        }
    }
}

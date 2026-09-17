package app.dao;

import app.entities.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class TenantDAO {
    private final EntityManagerFactory emf;

    public TenantDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<Tenant> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT t FROM Tenant t ORDER BY t.id", Tenant.class).getResultList();
        }
    }

    public Tenant getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Tenant.class, id);
        }
    }

    public Tenant create(Tenant tenant) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant saved = em.merge(tenant);
            em.getTransaction().commit();
            return saved;
        }
    }

    public Tenant update(Tenant tenant) {
        try (EntityManager em = emf.createEntityManager()) {
            Tenant existing = em.find(Tenant.class, tenant.getId());
            if (existing == null) return null;

            em.getTransaction().begin();
            existing.setName(tenant.getName());
            em.getTransaction().commit();
            return existing;
        }
    }

    public Tenant delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Tenant tenant = em.find(Tenant.class, id);
            if (tenant == null) return null;

            em.getTransaction().begin();
            em.remove(tenant);
            em.getTransaction().commit();
            return tenant;
        }
    }
}

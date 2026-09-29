package app.dao;

import app.entities.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.List;

public class ProductDAO {
    private final EntityManagerFactory emf;

    public ProductDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public Product save(Product product) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Product saved = em.merge(product);
            em.getTransaction().commit();
            return saved;
        }
    }

    public Product findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Product.class, id);
        }
    }

    public List<Product> findAll(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT p FROM Product p WHERE p.tenantId = :tenantId ORDER BY p.productNumber", Product.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public Product findByProductNumber(Long tenantId, String productNumber) {
        if (productNumber == null || productNumber.isBlank()) return null;
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.productNumber = :productNumber", Product.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("productNumber", productNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Product findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.idempotencyKey = :idempotencyKey", Product.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("idempotencyKey", idempotencyKey)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}

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

    public List<Product> findAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT p FROM Product p ORDER BY p.productNumber", Product.class).getResultList();
        }
    }

    public Product findByProductNumber(String productNumber) {
        if (productNumber == null || productNumber.isBlank()) return null;
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT p FROM Product p WHERE p.productNumber = :productNumber", Product.class)
                    .setParameter("productNumber", productNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Product findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT p FROM Product p WHERE p.idempotencyKey = :idempotencyKey", Product.class)
                    .setParameter("idempotencyKey", idempotencyKey)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}

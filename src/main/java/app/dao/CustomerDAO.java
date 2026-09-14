package app.dao;

import app.entities.Customer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

public class CustomerDAO {
    private final EntityManagerFactory emf;

    public CustomerDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public Customer save(Customer customer) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Customer saved = em.merge(customer);
            em.getTransaction().commit();
            return saved;
        }
    }

    public Customer findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Customer.class, id);
        }
    }

    public Customer findByEconomicCustomerNumber(Integer customerNumber) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT c FROM Customer c WHERE c.economicCustomerNumber = :customerNumber", Customer.class)
                    .setParameter("customerNumber", customerNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public boolean existsByEconomicCustomerNumber(Integer customerNumber) {
        return findByEconomicCustomerNumber(customerNumber) != null;
    }

    public Customer findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT c FROM Customer c WHERE c.idempotencyKey = :idempotencyKey", Customer.class)
                    .setParameter("idempotencyKey", idempotencyKey)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}

package app.dao;

import app.entities.Qualification;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QualificationDAO {
    private final EntityManagerFactory emf;

    public QualificationDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<Qualification> findByTenantId(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT q FROM Qualification q
                            WHERE q.tenantId = :tenantId
                            ORDER BY LOWER(q.name), q.id
                            """, Qualification.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public Qualification findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(Qualification.class, id);
        }
    }

    public Qualification findByName(Long tenantId, String name) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT q FROM Qualification q
                            WHERE q.tenantId = :tenantId AND LOWER(q.name) = LOWER(:name)
                            """, Qualification.class)
                    .setParameter("tenantId", tenantId)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Qualification save(Qualification qualification) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Qualification saved = qualification.getId() == null ? qualification : em.merge(qualification);
            if (qualification.getId() == null) {
                em.persist(qualification);
                saved = qualification;
            }
            em.getTransaction().commit();
            return saved;
        }
    }

    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Qualification qualification = em.find(Qualification.class, id);
            if (qualification == null) return;

            em.getTransaction().begin();
            em.createNativeQuery("DELETE FROM user_qualifications WHERE qualification_id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            em.remove(qualification);
            em.getTransaction().commit();
        }
    }

    public User replaceUserQualifications(Long userId, Set<Long> qualificationIds) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, userId);
            if (user == null) return null;
            Set<Qualification> qualifications = resolveQualifications(em, qualificationIds, user.getTenantId());

            em.getTransaction().begin();
            user.setQualifications(qualifications);
            em.getTransaction().commit();
            return user;
        }
    }

    public User addUserQualification(Long userId, Long qualificationId) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, userId);
            if (user == null) return null;
            Qualification qualification = resolveQualification(em, qualificationId, user.getTenantId());

            em.getTransaction().begin();
            user.addQualification(qualification);
            em.getTransaction().commit();
            return user;
        }
    }

    public User removeUserQualification(Long userId, Long qualificationId) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, userId);
            if (user == null) return null;

            em.getTransaction().begin();
            user.getQualifications().removeIf(qualification -> qualification.getId().equals(qualificationId));
            em.getTransaction().commit();
            return user;
        }
    }

    private Set<Qualification> resolveQualifications(EntityManager em, Set<Long> ids, Long tenantId) {
        Set<Qualification> qualifications = new HashSet<>();
        if (ids == null) return qualifications;
        for (Long id : ids) {
            qualifications.add(resolveQualification(em, id, tenantId));
        }
        return qualifications;
    }

    private Qualification resolveQualification(EntityManager em, Long id, Long tenantId) {
        Qualification qualification = id == null ? null : em.find(Qualification.class, id);
        if (qualification == null || !tenantId.equals(qualification.getTenantId())) {
            throw new IllegalArgumentException("Qualification " + id + " does not belong to the user's tenant");
        }
        return qualification;
    }
}

package app.dao;

import app.entities.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

public class RoleDAO implements IRoleDAO {
    private final EntityManagerFactory emf;

    public RoleDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Optional<Role> findById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return Optional.ofNullable(em.find(Role.class, id));
        }
    }

    @Override
    public List<Role> findByTenantId(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Role> query = em.createQuery(
                "SELECT r FROM Role r WHERE r.tenant.id = :tenantId", Role.class);
            query.setParameter("tenantId", tenantId);
            return query.getResultList();
        }
    }

    @Override
    public Role save(Role role) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Role saved;
            if (role.getId() == null) {
                em.persist(role);
                saved = role;
            } else {
                saved = em.merge(role);
            }
            em.getTransaction().commit();
            return saved;
        }
    }

    @Override
    public void delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Role role = em.find(Role.class, id);
            if (role != null) {
                em.getTransaction().begin();
                em.createNativeQuery("DELETE FROM user_custom_roles WHERE role_id = :id")
                        .setParameter("id", id)
                        .executeUpdate();
                em.remove(role);
                em.getTransaction().commit();
            }
        }
    }
}

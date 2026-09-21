package app.dao;

import app.entities.Role;
import app.entities.User;
import app.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UserDAO implements ISecurityDAO {
    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public List<User> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT u FROM User u ORDER BY u.id", User.class).getResultList();
        }
    }

    public User getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(User.class, id);
        }
    }

    public User getByEmail(String email) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<User> getByTenantId(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("SELECT u FROM User u WHERE u.tenantId = :tenantId ORDER BY u.id", User.class)
                    .setParameter("tenantId", tenantId)
                    .getResultList();
        }
    }

    public User create(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            if (user.getPassword() != null && !user.getPassword().startsWith("$2")) {
                user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
            }
            User saved = em.merge(user);
            em.getTransaction().commit();
            return saved;
        }
    }

    public User update(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            User existing = em.find(User.class, user.getId());
            if (existing == null) return null;

            em.getTransaction().begin();
            applyFields(existing, user);
            em.getTransaction().commit();
            return existing;
        }
    }

    /**
     * Like {@link #update(User)}, but also replaces the user's custom roles when
     * {@code customRoleIds} is non-null. {@code null} leaves the assignments untouched,
     * an empty set clears them.
     */
    public User update(User user, Set<Long> customRoleIds) throws ValidationException {
        try (EntityManager em = emf.createEntityManager()) {
            User existing = em.find(User.class, user.getId());
            if (existing == null) return null;

            Set<Role> resolved = customRoleIds == null
                    ? null
                    : resolveRoles(em, customRoleIds, user.getTenantId());

            em.getTransaction().begin();
            applyFields(existing, user);
            if (resolved != null) {
                existing.setCustomRoles(resolved);
            }
            em.getTransaction().commit();
            return existing;
        }
    }

    private void applyFields(User existing, User user) {
        existing.setEmail(user.getEmail());
        existing.setPhoneNumber(user.getPhoneNumber());
        existing.setTenantId(user.getTenantId());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        }
        existing.setRoles(user.getRoles());
    }

    public User delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            if (user == null) return null;

            em.getTransaction().begin();
            em.remove(user);
            em.getTransaction().commit();
            return user;
        }
    }

    public User setRole(Long id, String role) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            if (user == null) return null;

            em.getTransaction().begin();
            user.replaceRole(role);
            em.getTransaction().commit();
            return user;
        }
    }

    public User setCustomRoles(Long id, Set<Long> roleIds) throws ValidationException {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            if (user == null) return null;

            Set<Role> resolved = resolveRoles(em, roleIds, user.getTenantId());

            em.getTransaction().begin();
            user.setCustomRoles(resolved);
            em.getTransaction().commit();
            return user;
        }
    }

    public User addCustomRole(Long id, Long roleId) throws ValidationException {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            if (user == null) return null;

            Role role = resolveRoles(em, Set.of(roleId), user.getTenantId()).iterator().next();

            em.getTransaction().begin();
            user.addCustomRole(role);
            em.getTransaction().commit();
            return user;
        }
    }

    public User removeCustomRole(Long id, Long roleId) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, id);
            if (user == null) return null;

            em.getTransaction().begin();
            user.getCustomRoles().removeIf(role -> role.getId().equals(roleId));
            em.getTransaction().commit();
            return user;
        }
    }

    private Set<Role> resolveRoles(EntityManager em, Set<Long> ids, Long tenantId) throws ValidationException {
        Set<Role> roles = new HashSet<>();
        if (ids == null) return roles;
        for (Long roleId : ids) {
            Role role = roleId == null ? null : em.find(Role.class, roleId);
            if (role == null) {
                throw new ValidationException("Role " + roleId + " does not exist");
            }
            if (tenantId == null || !tenantId.equals(role.getTenant().getId())) {
                throw new ValidationException("Role " + roleId + " does not belong to the user's tenant");
            }
            roles.add(role);
        }
        return roles;
    }

    @Override
    public User createUser(String email, String password) throws ValidationException {
        return createUser(email, password, null, null);
    }

    @Override
    public User createUser(String email, String password, String phoneNumber, Long tenantId) throws ValidationException {
        validateCredentials(email, password);
        if (getByEmail(email) != null) {
            throw new ValidationException("User already exists");
        }
        return create(new User(null, email, password, phoneNumber, tenantId, java.util.Set.of("USER")));
    }

    @Override
    public User getVerifiedUser(String email, String password) throws ValidationException {
        validateCredentials(email, password);
        User user = Optional.ofNullable(getByEmail(email))
                .orElseThrow(() -> new ValidationException("Invalid email or password"));
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ValidationException("Invalid email or password");
        }
        return user;
    }

    @Override
    public void changePassword(String email, String currentPassword, String newPassword) throws ValidationException {
        User user = getVerifiedUser(email, currentPassword);
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password is required");
        }

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User managed = em.find(User.class, user.getId());
            managed.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            em.getTransaction().commit();
        }
    }

    private void validateCredentials(String email, String password) throws ValidationException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Email and password are required");
        }
    }

    public User deactivateUser(long userID) {
        try (EntityManager em = emf.createEntityManager()) {
            User user = em.find(User.class, userID);
            user.setIsActive(false);
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
            return user;
        } catch (Exception e) {
        }
        return null;
    }
}

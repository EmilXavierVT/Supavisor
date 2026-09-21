package app.dao;

import app.config.TestEntityManagerFactory;
import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UserCustomRolesIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;
    private static UserDAO userDAO;
    private static RoleDAO roleDAO;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
        userDAO = new UserDAO(emf);
        roleDAO = new RoleDAO(emf);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void rolesAndJoinTableAreCreated() {
        try (EntityManager em = emf.createEntityManager()) {
            for (String table : new String[]{"roles", "user_custom_roles"}) {
                Number count = (Number) em.createNativeQuery(
                                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :name")
                        .setParameter("name", table)
                        .getSingleResult();
                assertEquals(1, count.intValue(), "missing table " + table);
            }
        }
    }

    @Test
    void assignedCustomRolesAreReloadedAndSystemRolesAreUntouched() throws ValidationException {
        Tenant tenant = createTenant();
        Role cleaning = createRole(tenant, "Cleaning");
        Role kitchen = createRole(tenant, "Kitchen");
        User user = createUser(tenant);

        userDAO.setCustomRoles(user.getId(), Set.of(cleaning.getId(), kitchen.getId()));

        User reloaded = userDAO.getById(user.getId());
        assertEquals(Set.of("Cleaning", "Kitchen"), roleNames(reloaded));
        assertEquals(Set.of("USER"), reloaded.getRoles());
    }

    @Test
    void addAndRemoveSingleCustomRole() throws ValidationException {
        Tenant tenant = createTenant();
        Role cleaning = createRole(tenant, "Cleaning");
        Role kitchen = createRole(tenant, "Kitchen");
        User user = createUser(tenant);

        userDAO.addCustomRole(user.getId(), cleaning.getId());
        userDAO.addCustomRole(user.getId(), kitchen.getId());
        assertEquals(Set.of("Cleaning", "Kitchen"), roleNames(userDAO.getById(user.getId())));

        userDAO.removeCustomRole(user.getId(), cleaning.getId());
        assertEquals(Set.of("Kitchen"), roleNames(userDAO.getById(user.getId())));
    }

    @Test
    void assigningRoleFromAnotherTenantIsRejected() {
        Tenant tenant = createTenant();
        Tenant other = createTenant();
        Role foreign = createRole(other, "Cleaning");
        User user = createUser(tenant);

        assertThrows(ValidationException.class,
                () -> userDAO.setCustomRoles(user.getId(), Set.of(foreign.getId())));
        assertThrows(ValidationException.class,
                () -> userDAO.addCustomRole(user.getId(), foreign.getId()));
        assertTrue(userDAO.getById(user.getId()).getCustomRoles().isEmpty());
    }

    @Test
    void assigningUnknownRoleIsRejected() {
        User user = createUser(createTenant());

        assertThrows(ValidationException.class,
                () -> userDAO.setCustomRoles(user.getId(), Set.of(-1L)));
    }

    @Test
    void deletingAnAssignedRoleUnassignsItAndKeepsTheUser() throws ValidationException {
        Tenant tenant = createTenant();
        Role cleaning = createRole(tenant, "Cleaning");
        Role kitchen = createRole(tenant, "Kitchen");
        User user = createUser(tenant);
        userDAO.setCustomRoles(user.getId(), Set.of(cleaning.getId(), kitchen.getId()));

        roleDAO.delete(cleaning.getId());

        assertTrue(roleDAO.findById(cleaning.getId()).isEmpty());
        User reloaded = userDAO.getById(user.getId());
        assertNotNull(reloaded);
        assertEquals(Set.of("Kitchen"), roleNames(reloaded));
        assertEquals(Set.of("USER"), reloaded.getRoles());
    }

    @Test
    void updateWithNullRoleIdsPreservesAndEmptySetClears() throws ValidationException {
        Tenant tenant = createTenant();
        Role cleaning = createRole(tenant, "Cleaning");
        User user = createUser(tenant);
        userDAO.setCustomRoles(user.getId(), Set.of(cleaning.getId()));

        User changes = new User(user.getId(), user.getEmail(), null, "12345678", tenant.getId(), Set.of("USER"));

        userDAO.update(changes, null);
        User preserved = userDAO.getById(user.getId());
        assertEquals("12345678", preserved.getPhoneNumber());
        assertEquals(Set.of("Cleaning"), roleNames(preserved));

        userDAO.update(changes, Set.of());
        assertTrue(userDAO.getById(user.getId()).getCustomRoles().isEmpty());
    }

    private static Set<String> roleNames(User user) {
        return user.getCustomRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
    }

    private static Tenant createTenant() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant tenant = new Tenant(null, "tenant-" + UUID.randomUUID());
            em.persist(tenant);
            em.getTransaction().commit();
            return tenant;
        }
    }

    private static Role createRole(Tenant tenant, String name) {
        return roleDAO.save(Role.builder().roleName(name).tenant(tenant).build());
    }

    private static User createUser(Tenant tenant) {
        return userDAO.create(new User(null, UUID.randomUUID() + "@example.com", "secret-password",
                null, tenant.getId(), Set.of("USER")));
    }
}

package app.services.entityServices;

import app.config.TestEntityManagerFactory;
import app.dao.RoleDAO;
import app.dao.UserDAO;
import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.exceptions.DuplicateUserException;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UserServiceIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;
    private static UserService userService;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
        userService = new UserService(emf);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void createsActiveUserWithNameRoleTenantAndWorkingTemporaryPassword() throws ValidationException {
        UserService.CreatedUser created =
                userService.createUserWithRole("  Jane Doe ", " jane@example.com ", "user", 5L, null);

        User user = created.user();
        assertNotNull(user.getId());
        assertEquals("Jane Doe", user.getName());
        assertEquals("jane@example.com", user.getEmail());
        assertEquals(Set.of("USER"), user.getRoles());
        assertTrue(user.getCustomRoles().isEmpty());
        assertEquals(5L, user.getTenantId());
        assertTrue(user.getIsActive());
        assertNotNull(new UserDAO(emf).getVerifiedUser("jane@example.com", created.temporaryPassword()));
    }

    @Test
    void rejectsEmailAlreadyInUseIgnoringCase() throws ValidationException {
        userService.createUserWithRole("First", "taken@example.com", "USER", 5L, null);

        assertThrows(DuplicateUserException.class,
                () -> userService.createUserWithRole("Second", "TAKEN@example.com", "USER", 5L, null));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("", "a@example.com", "USER", 5L, null));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "not-an-email", "USER", 5L, null));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "b@example.com", "EMPLOYEE", 5L, null));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "c@example.com", null, 5L, null));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "d@example.com", "USER", null, null));
    }

    @Test
    void updateWithoutRolesKeepsExistingRoles() throws ValidationException {
        User admin = userService.createUserWithRole("Admin", "keeps-role@example.com", "ADMIN", 5L, null).user();

        User changes = new User(admin.getId(), "keeps-role@example.com", null, "12345678", 5L, Set.of());
        userService.update(changes);

        assertEquals(Set.of("ADMIN"), userService.getById(admin.getId()).getRoles());
    }

    @Test
    void assignsOnlyTheChosenCustomRolesOfTheTenant() throws ValidationException {
        Tenant tenant = createTenant();
        Role kitchen = createRole(tenant, "Kitchen");
        createRole(tenant, "Cleaning");

        User user = userService.createUserWithRole("Cook", "cook@example.com", "USER", tenant.getId(),
                Set.of(kitchen.getId())).user();

        assertEquals(Set.of("Kitchen"), user.getCustomRoles().stream().map(Role::getRoleName).collect(Collectors.toSet()));
    }

    @Test
    void rejectsCustomRolesFromAnotherTenantAndCreatesNothing() {
        Tenant mine = createTenant();
        Tenant other = createTenant();
        Role foreign = createRole(other, "Kitchen");

        assertThrows(ValidationException.class, () -> userService.createUserWithRole(
                "Cook", "foreign-role@example.com", "USER", mine.getId(), Set.of(foreign.getId())));
        assertNull(userService.getByEmail("foreign-role@example.com"));
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
        return new RoleDAO(emf).save(Role.builder().roleName(name).tenant(tenant).build());
    }
}

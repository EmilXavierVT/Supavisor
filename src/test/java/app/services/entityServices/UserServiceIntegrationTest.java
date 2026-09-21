package app.services.entityServices;

import app.config.TestEntityManagerFactory;
import app.dao.UserDAO;
import app.entities.User;
import app.exceptions.DuplicateUserException;
import app.exceptions.ValidationException;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                userService.createUserWithRole("  Jane Doe ", " jane@example.com ", "employee", 5L);

        User user = created.user();
        assertNotNull(user.getId());
        assertEquals("Jane Doe", user.getName());
        assertEquals("jane@example.com", user.getEmail());
        assertEquals(Set.of("EMPLOYEE"), user.getRoles());
        assertEquals(5L, user.getTenantId());
        assertTrue(user.getIsActive());
        assertNotNull(new UserDAO(emf).getVerifiedUser("jane@example.com", created.temporaryPassword()));
    }

    @Test
    void rejectsEmailAlreadyInUseIgnoringCase() throws ValidationException {
        userService.createUserWithRole("First", "taken@example.com", "FLEX", 5L);

        assertThrows(DuplicateUserException.class,
                () -> userService.createUserWithRole("Second", "TAKEN@example.com", "FLEX", 5L));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("", "a@example.com", "FLEX", 5L));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "not-an-email", "FLEX", 5L));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "b@example.com", "USER", 5L));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "c@example.com", null, 5L));
        assertThrows(ValidationException.class, () -> userService.createUserWithRole("A", "d@example.com", "FLEX", null));
    }

    @Test
    void updateWithoutRolesKeepsExistingRoles() throws ValidationException {
        User admin = userService.createUserWithRole("Admin", "keeps-role@example.com", "ADMIN", 5L).user();

        User changes = new User(admin.getId(), "keeps-role@example.com", null, "12345678", 5L, Set.of());
        userService.update(changes);

        assertEquals(Set.of("ADMIN"), userService.getById(admin.getId()).getRoles());
    }
}

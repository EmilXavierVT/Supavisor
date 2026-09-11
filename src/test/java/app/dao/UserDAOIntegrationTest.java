package app.dao;

import app.config.TestEntityManagerFactory;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class UserDAOIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void hibernateCanConnectToPostgresAndCreateUserTable() {
        try (EntityManager em = emf.createEntityManager()) {
            Integer result = ((Number) em.createNativeQuery("SELECT 1").getSingleResult()).intValue();

            assertEquals(1, result);
        }
    }

    @Test
    void userDaoCanCreateAndFetchUserFromPostgres() throws ValidationException {
        UserDAO userDAO = new UserDAO(emf);

        User created = userDAO.createUser("employee@example.com", "secret-password");
        User fetched = userDAO.getByEmail("employee@example.com");

        assertNotNull(created.getId());
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("employee@example.com", fetched.getEmail());
        assertEquals(Set.of("USER"), fetched.getRoles());
        assertTrue(userDAO.getVerifiedUser("employee@example.com", "secret-password").getRoles().contains("USER"));
    }
}

package app.dao;

import app.config.TestEntityManagerFactory;
import app.entities.Customer;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class CustomerDAOIntegrationTest {
    private static final Long TENANT_ID = 1L;
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_customer_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) emf.close();
    }

    @Test
    void savesAndFindsCustomerByIdEconomicNumberAndIdempotencyKey() {
        CustomerDAO dao = new CustomerDAO(emf);
        Customer customer = new Customer(null, TENANT_ID, "Example Company", "billing@example.com", "Street 1", "2100",
                "Copenhagen", "Denmark", "12345678", "DKK", 1001, "idem-1", null, null);

        Customer saved = dao.save(customer);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getId(), dao.findById(saved.getId()).getId());
        assertEquals(saved.getId(), dao.findByEconomicCustomerNumber(TENANT_ID, 1001).getId());
        assertEquals(saved.getId(), dao.findByIdempotencyKey(TENANT_ID, "idem-1").getId());
        assertTrue(dao.existsByEconomicCustomerNumber(TENANT_ID, 1001));
    }
}

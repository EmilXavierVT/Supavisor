package app.dao;

import app.config.TestEntityManagerFactory;
import app.entities.Product;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ProductDAOIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_product_test")
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
    void savesAndFindsProductByIdProductNumberAndIdempotencyKey() {
        ProductDAO dao = new ProductDAO(emf);
        Product product = new Product(null, "P-1", "Example Product", "Description", new BigDecimal("100.00"),
                new BigDecimal("50.00"), new BigDecimal("120.00"), "1234567890", false, null,
                1, "Goods", 2, "pcs", "https://restapi.e-conomic.com/products/P-1", "idem-product-1", null, null);

        Product saved = dao.save(product);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getId(), dao.findById(saved.getId()).getId());
        assertEquals(saved.getId(), dao.findByProductNumber("P-1").getId());
        assertEquals(saved.getId(), dao.findByIdempotencyKey("idem-product-1").getId());
        assertEquals(1, dao.findAll().size());
    }
}

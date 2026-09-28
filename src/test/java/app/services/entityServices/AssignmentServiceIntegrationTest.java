package app.services.entityServices;

import app.config.TestEntityManagerFactory;
import app.dao.UserDAO;
import app.dto.AssignmentDTO;
import app.entities.Assignment;
import app.entities.User;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class AssignmentServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;
    private static AssignmentService service;
    private static final String TEST_USER = "TEST_RUNNER";

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
        service = new AssignmentService(emf);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) {
            emf.close();
        }
    }

    // ---- create / read

    @Test
    void createStoresAnActiveAssignmentInTheDatabase() {
        Long tenant = newTenantId();

        Assignment created = service.create(entity("Cleaning", tenant), TEST_USER);

        assertNotNull(created.getId());
        assertEquals("Cleaning", created.getName());
        assertEquals(tenant, created.getTenantId());
        assertTrue(created.isActive());
        Object[] row = dbRow(created.getId());
        assertEquals("Cleaning", row[0]);
        assertEquals(true, row[1]);
        assertEquals(tenant, ((Number) row[2]).longValue());
    }

    @Test
    void createCanStartDeactivated() {
        Long tenant = newTenantId();

        Assignment created = service.create(entity("Archived work", tenant, false), TEST_USER);

        assertFalse(created.isActive());
        assertEquals(false, dbRow(created.getId())[1]);
    }

    @Test
    void readReturnsTheAssignmentAndListShowsAllOfTheTenant() {
        Long tenant = newTenantId();
        Assignment cleaning = service.create(entity("Cleaning", tenant), TEST_USER);
        service.create(entity("Kitchen", tenant), TEST_USER);

        assertEquals("Cleaning", service.getById(cleaning.getId(), tenant).getName());
        assertEquals(List.of("Cleaning", "Kitchen"), names(service.getAll(tenant, false)));
    }

    @Test
    void readOfUnknownAssignmentIsNotFound() {
        assertEquals(404, code(() -> service.getById(-1L, newTenantId())));
    }

    // ---- validation and uniqueness

    @Test
    void missingOrInvalidNameIsRejectedAndNothingIsStored() {
        Long tenant = newTenantId();

        assertEquals(400, code(() -> service.create(entity(null, tenant), TEST_USER)));
        assertEquals(400, code(() -> service.create(entity("", tenant), TEST_USER)));
        assertEquals(400, code(() -> service.create(entity("   ", tenant), TEST_USER)));
        assertEquals(400, code(() -> service.create(entity("a".repeat(101), tenant), TEST_USER)));
        assertEquals(400, code(() -> service.create(entity("bad\u0000name", tenant), TEST_USER)));
        assertTrue(service.getAll(tenant, false).isEmpty());
    }

    @Test
    void nameAlreadyInUseIsRejectedIgnoringCaseAndSpaces() {
        Long tenant = newTenantId();
        service.create(entity("Cleaning", tenant), TEST_USER);

        assertEquals(409, code(() -> service.create(entity("cleaning", tenant), TEST_USER)));
        assertEquals(409, code(() -> service.create(entity("  CLEANING  ", tenant), TEST_USER)));
        assertEquals(1, service.getAll(tenant, false).size());
    }

    @Test
    void sameNameIsAllowedInAnotherTenant() {
        service.create(entity("Cleaning", newTenantId()), TEST_USER);

        assertEquals("Cleaning", service.create(entity("Cleaning", newTenantId()), TEST_USER).getName());
    }

    // ---- update

    @Test
    void updateChangesNameAndActiveFlagInTheDatabase() {
        Long tenant = newTenantId();
        Assignment created = service.create(entity("Cleaning", tenant), TEST_USER);

        created.setName("Deep cleaning");
        created.setActive(false);
        Assignment updated = service.update(created, TEST_USER);

        assertEquals("Deep cleaning", updated.getName());
        assertFalse(updated.isActive());
        Object[] row = dbRow(created.getId());
        assertEquals("Deep cleaning", row[0]);
        assertEquals(false, row[1]);
    }

    @Test
    void updateRejectsInvalidAndDuplicateNamesAndLeavesTheRowUntouched() {
        Long tenant = newTenantId();
        Assignment cleaning = service.create(entity("Cleaning", tenant), TEST_USER);
        service.create(entity("Kitchen", tenant), TEST_USER);

        cleaning.setName(" ");
        assertEquals(400, code(() -> service.update(cleaning, TEST_USER)));

        cleaning.setName("KITCHEN");
        assertEquals(409, code(() -> service.update(cleaning, TEST_USER)));

        assertEquals("Cleaning", dbRow(cleaning.getId())[0]);
    }

    // ---- deactivate / cancel

    @Test
    void deactivateKeepsTheRowButHidesItFromTheActiveList() {
        Long tenant = newTenantId();
        Assignment cleaning = service.create(entity("Cleaning", tenant), TEST_USER);
        service.create(entity("Kitchen", tenant), TEST_USER);

        assertFalse(service.cancel(cleaning.getId(), tenant, TEST_USER).isActive());

        assertEquals(List.of("Kitchen"), names(service.getAll(tenant, true)));
        assertEquals(List.of("Cleaning", "Kitchen"), names(service.getAll(tenant, false)));
        assertEquals(false, dbRow(cleaning.getId())[1]);
        assertEquals("Cleaning", service.getById(cleaning.getId(), tenant).getName());
    }

    // ---- delete

    @Test
    void deleteRemovesTheRowFromTheDatabase() {
        Long tenant = newTenantId();
        Assignment created = service.create(entity("Cleaning", tenant), TEST_USER);

        service.delete(created.getId(), tenant, TEST_USER);

        assertEquals(0, dbCount(created.getId()));
        assertEquals(404, code(() -> service.getById(created.getId(), tenant)));
        assertEquals(404, code(() -> service.delete(created.getId(), tenant, TEST_USER)));
    }

    // ---- details: address, estimated time, cost, employee

    @Test
    void createStoresAddressEstimatedTimeCostAndEmployeeInTheDatabase() {
        Long tenant = newTenantId();
        User employee = newEmployee(tenant, true);

        Assignment assignment = entity("Cleaning at Main Street", tenant);
        assignment.setAddress("Main Street 1, Aarhus");
        assignment.setEstimatedMinutes(90);
        assignment.setCost(new BigDecimal("1250.50"));
        assignment.setAssignedEmployeeId(employee.getId());

        Assignment created = service.create(assignment, TEST_USER);

        assertEquals("Main Street 1, Aarhus", created.getAddress());
        assertEquals(90, created.getEstimatedMinutes());
        assertEquals(0, new BigDecimal("1250.50").compareTo(created.getCost()));
        assertEquals(employee.getId(), created.getAssignedEmployeeId());
    }

    // ---- helpers

    private static Assignment entity(String name, Long tenantId) {
        return entity(name, tenantId, true);
    }

    private static Assignment entity(String name, Long tenantId, boolean active) {
        Assignment a = new Assignment();
        a.setName(name == null ? null : name.trim());
        a.setTenantId(tenantId);
        a.setActive(active);
        return a;
    }

    private static User newEmployee(Long tenantId, boolean active) {
        return new UserDAO(emf).create(new User(null, UUID.randomUUID() + "@example.com", "secret-password",
                null, tenantId, active, Set.of("USER")));
    }

    private static List<String> names(List<Assignment> list) {
        return list.stream().map(Assignment::getName).toList();
    }

    private static Long newTenantId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L) + 1;
    }

    private static int code(Runnable action) {
        return assertThrows(ApiException.class, action::run).getCode();
    }

    private static Object[] dbRow(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return (Object[]) em.createNativeQuery("SELECT name, is_active, tenant_id FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        }
    }

    private static int dbCount(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult()).intValue();
        }
    }
}
package app.services.entityServices;

import app.config.TestEntityManagerFactory;
import app.dto.AssignmentDTO;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        AssignmentDTO created = service.create(dto("  Cleaning "), tenant);

        assertNotNull(created.getId());
        assertEquals("Cleaning", created.getName());
        assertEquals(tenant, created.getTenantId());
        assertTrue(created.getIsActive());
        Object[] row = dbRow(created.getId());
        assertEquals("Cleaning", row[0]);
        assertEquals(true, row[1]);
        assertEquals(tenant, ((Number) row[2]).longValue());
    }

    @Test
    void createCanStartDeactivated() {
        Long tenant = newTenantId();

        AssignmentDTO created = service.create(dto("Archived work", false), tenant);

        assertFalse(created.getIsActive());
        assertEquals(false, dbRow(created.getId())[1]);
    }

    @Test
    void readReturnsTheAssignmentAndListShowsAllOfTheTenant() {
        Long tenant = newTenantId();
        AssignmentDTO cleaning = service.create(dto("Cleaning"), tenant);
        service.create(dto("Kitchen"), tenant);

        assertEquals("Cleaning", service.getById(cleaning.getId(), tenant, false).getName());
        assertEquals(List.of("Cleaning", "Kitchen"), names(service.getAll(tenant, false)));
    }

    @Test
    void readOfUnknownAssignmentIsNotFound() {
        assertEquals(404, code(() -> service.getById(-1L, newTenantId(), false)));
    }

    // ---- validation and uniqueness

    @Test
    void missingOrInvalidNameIsRejectedAndNothingIsStored() {
        Long tenant = newTenantId();

        assertEquals(400, code(() -> service.create(dto(null), tenant)));
        assertEquals(400, code(() -> service.create(dto(""), tenant)));
        assertEquals(400, code(() -> service.create(dto("   "), tenant)));
        assertEquals(400, code(() -> service.create(dto("a".repeat(101)), tenant)));
        assertEquals(400, code(() -> service.create(dto("bad\u0000name"), tenant)));
        assertTrue(service.getAll(tenant, false).isEmpty());
    }

    @Test
    void nameAlreadyInUseIsRejectedIgnoringCaseAndSpaces() {
        Long tenant = newTenantId();
        service.create(dto("Cleaning"), tenant);

        assertEquals(409, code(() -> service.create(dto("cleaning"), tenant)));
        assertEquals(409, code(() -> service.create(dto("  CLEANING  "), tenant)));
        assertEquals(1, service.getAll(tenant, false).size());
    }

    @Test
    void sameNameIsAllowedInAnotherTenant() {
        service.create(dto("Cleaning"), newTenantId());

        assertEquals("Cleaning", service.create(dto("Cleaning"), newTenantId()).getName());
    }

    // ---- update

    @Test
    void updateChangesNameAndActiveFlagInTheDatabase() {
        Long tenant = newTenantId();
        AssignmentDTO created = service.create(dto("Cleaning"), tenant);

        AssignmentDTO updated = service.update(created.getId(), dto("Deep cleaning", false), tenant);

        assertEquals("Deep cleaning", updated.getName());
        assertFalse(updated.getIsActive());
        Object[] row = dbRow(created.getId());
        assertEquals("Deep cleaning", row[0]);
        assertEquals(false, row[1]);
    }

    @Test
    void updateWithoutActiveFlagKeepsItAndKeepingOwnNameIsNotADuplicate() {
        Long tenant = newTenantId();
        AssignmentDTO created = service.create(dto("Cleaning", false), tenant);

        AssignmentDTO updated = service.update(created.getId(), dto("cleaning"), tenant);

        assertEquals("cleaning", updated.getName());
        assertFalse(updated.getIsActive());
    }

    @Test
    void updateRejectsInvalidAndDuplicateNamesAndLeavesTheRowUntouched() {
        Long tenant = newTenantId();
        AssignmentDTO cleaning = service.create(dto("Cleaning"), tenant);
        service.create(dto("Kitchen"), tenant);

        assertEquals(400, code(() -> service.update(cleaning.getId(), dto(" "), tenant)));
        assertEquals(409, code(() -> service.update(cleaning.getId(), dto("KITCHEN"), tenant)));
        assertEquals(404, code(() -> service.update(-1L, dto("Anything"), tenant)));
        assertEquals("Cleaning", dbRow(cleaning.getId())[0]);
    }

    // ---- deactivate

    @Test
    void deactivateKeepsTheRowButHidesItFromTheActiveList() {
        Long tenant = newTenantId();
        AssignmentDTO cleaning = service.create(dto("Cleaning"), tenant);
        service.create(dto("Kitchen"), tenant);

        assertFalse(service.deactivate(cleaning.getId(), tenant).getIsActive());
        assertFalse(service.deactivate(cleaning.getId(), tenant).getIsActive()); // idempotent

        assertEquals(List.of("Kitchen"), names(service.getAll(tenant, true)));
        assertEquals(List.of("Cleaning", "Kitchen"), names(service.getAll(tenant, false)));
        assertEquals(false, dbRow(cleaning.getId())[1]);
        assertEquals(404, code(() -> service.getById(cleaning.getId(), tenant, true)));
        assertEquals("Cleaning", service.getById(cleaning.getId(), tenant, false).getName());
    }

    // ---- delete

    @Test
    void deleteRemovesTheRowFromTheDatabase() {
        Long tenant = newTenantId();
        AssignmentDTO created = service.create(dto("Cleaning"), tenant);

        service.delete(created.getId(), tenant);

        assertEquals(0, dbCount(created.getId()));
        assertEquals(404, code(() -> service.getById(created.getId(), tenant, false)));
        assertEquals(404, code(() -> service.delete(created.getId(), tenant)));
    }

    @Test
    void deleteIsRefusedWhileAnotherRecordReferencesTheAssignment() {
        Long tenant = newTenantId();
        AssignmentDTO created = service.create(dto("Cleaning"), tenant);
        // stands in for any future table with a foreign key to assignments
        execute("CREATE TABLE IF NOT EXISTS assignment_usage_probe "
                + "(id SERIAL PRIMARY KEY, assignment_id BIGINT NOT NULL REFERENCES assignments(id))");
        execute("INSERT INTO assignment_usage_probe (assignment_id) VALUES (" + created.getId() + ")");
        try {
            assertEquals(409, code(() -> service.delete(created.getId(), tenant)));
            assertEquals(1, dbCount(created.getId()));

            // it can still be deactivated, and it keeps existing for whoever references it
            assertFalse(service.deactivate(created.getId(), tenant).getIsActive());
            assertEquals(1, dbCount(created.getId()));
        } finally {
            execute("DELETE FROM assignment_usage_probe");
        }
        service.delete(created.getId(), tenant);
        assertEquals(0, dbCount(created.getId()));
    }

    // ---- tenant isolation

    @Test
    void anotherTenantCannotSeeChangeDeactivateOrDelete() {
        Long mine = newTenantId();
        Long other = newTenantId();
        AssignmentDTO created = service.create(dto("Cleaning"), mine);

        assertTrue(service.getAll(other, false).isEmpty());
        assertEquals(404, code(() -> service.getById(created.getId(), other, false)));
        assertEquals(404, code(() -> service.update(created.getId(), dto("Hacked"), other)));
        assertEquals(404, code(() -> service.deactivate(created.getId(), other)));
        assertEquals(404, code(() -> service.delete(created.getId(), other)));
        assertEquals("Cleaning", dbRow(created.getId())[0]);
        assertEquals(true, dbRow(created.getId())[1]);
    }

    // ---- helpers

    private static AssignmentDTO dto(String name) {
        return dto(name, null);
    }

    private static AssignmentDTO dto(String name, Boolean isActive) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setName(name);
        dto.setIsActive(isActive);
        return dto;
    }

    private static List<String> names(List<AssignmentDTO> list) {
        return list.stream().map(AssignmentDTO::getName).toList();
    }

    /** Each test gets its own tenant so tests cannot see each other's rows. */
    private static Long newTenantId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L) + 1;
    }

    private static int code(Runnable action) {
        return assertThrows(ApiException.class, action::run).getCode();
    }

    /** name, is_active, tenant_id straight from the table, bypassing the service. */
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

    private static void execute(String sql) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createNativeQuery(sql).executeUpdate();
            em.getTransaction().commit();
        }
    }
}

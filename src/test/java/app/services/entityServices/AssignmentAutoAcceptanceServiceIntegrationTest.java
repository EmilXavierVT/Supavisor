package app.services.entityServices;

import app.config.TestEntityManagerFactory;
import app.dao.AssignmentDAO;
import app.dao.UserDAO;
import app.dto.AssignmentDTO;
import app.entities.AssignmentState;
import app.entities.AssignmentStateHistory;
import app.entities.User;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class AssignmentAutoAcceptanceServiceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_auto_acceptance_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void setUp() {
        entityManagerFactory = TestEntityManagerFactory.create(POSTGRES);
    }

    @AfterAll
    static void tearDown() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    void dueAssignmentIsAcceptedOnceWithSystemHistory() {
        Long tenantId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        User employee = new UserDAO(entityManagerFactory).create(User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .password("secret-password")
                .tenantId(tenantId)
                .isActive(true)
                .roles(Set.of("USER"))
                .build());
        AssignmentDTO request = new AssignmentDTO();
        request.setName("Kitchen preparation");
        request.setAssignedEmployeeId(employee.getId());
        request.setStartTime(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        AssignmentDTO assignment = new AssignmentService(entityManagerFactory).create(request, tenantId);
        AssignmentAutoAcceptanceService service = new AssignmentAutoAcceptanceService(
                entityManagerFactory,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertEquals(1, service.acceptDueAssignments());
        assertEquals(0, service.acceptDueAssignments());

        AssignmentDAO assignmentDAO = new AssignmentDAO(entityManagerFactory);
        assertEquals(AssignmentState.AUTO_ACCEPTED, assignmentDAO.getById(assignment.getId()).getState());
        List<AssignmentStateHistory> history = assignmentDAO.getStateHistory(assignment.getId());
        assertEquals(AssignmentState.AUTO_ACCEPTED, history.get(0).getToState());
        assertEquals("system:auto-accept", history.get(0).getSource());
        assertEquals(NOW, history.get(0).getChangedAt());
    }
}

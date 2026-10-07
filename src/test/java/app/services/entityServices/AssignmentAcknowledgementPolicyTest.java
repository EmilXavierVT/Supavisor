package app.services.entityServices;

import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.User;
import app.exceptions.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentAcknowledgementPolicyTest {

    private static final Long TENANT_ID = 10L;
    private static final Long EMPLOYEE_ID = 20L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Test
    void assignedEmployeeCanAcknowledgeAPlannedFutureAssignment() {
        Assignment assignment = assignment(EMPLOYEE_ID, AssignmentState.PLANNED, NOW.plusHours(1));

        assertDoesNotThrow(() -> AssignmentAcknowledgementPolicy.validate(
                assignment,
                TENANT_ID,
                EMPLOYEE_ID,
                NOW
        ));
    }

    @Test
    void anotherEmployeeCannotAcknowledgeTheAssignment() {
        Assignment assignment = assignment(EMPLOYEE_ID, AssignmentState.PLANNED, NOW.plusHours(1));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> AssignmentAcknowledgementPolicy.validate(assignment, TENANT_ID, 999L, NOW)
        );

        assertEquals(404, exception.getCode());
    }

    @Test
    void acknowledgementIsRejectedAfterTheAssignmentStarts() {
        Assignment assignment = assignment(EMPLOYEE_ID, AssignmentState.PLANNED, NOW);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> AssignmentAcknowledgementPolicy.validate(assignment, TENANT_ID, EMPLOYEE_ID, NOW)
        );

        assertEquals(409, exception.getCode());
    }

    @Test
    void acknowledgementIsRejectedWhenTheAssignmentAlreadyChangedState() {
        Assignment assignment = assignment(EMPLOYEE_ID, AssignmentState.ACKNOWLEDGED, NOW.plusHours(1));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> AssignmentAcknowledgementPolicy.validate(assignment, TENANT_ID, EMPLOYEE_ID, NOW)
        );

        assertEquals(409, exception.getCode());
    }

    private static Assignment assignment(Long employeeId, AssignmentState state, LocalDateTime startTime) {
        User employee = new User();
        employee.setId(employeeId);

        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setTenantId(TENANT_ID);
        assignment.setActive(true);
        assignment.setAssignedEmployee(employee);
        assignment.setState(state);
        assignment.setStartTime(startTime);
        return assignment;
    }
}

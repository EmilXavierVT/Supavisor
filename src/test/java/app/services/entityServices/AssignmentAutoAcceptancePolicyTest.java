package app.services.entityServices;

import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentAutoAcceptancePolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Test
    void unacknowledgedAssignedWorkIsEligibleAtItsStartTime() {
        Assignment assignment = assignment(AssignmentState.PLANNED, NOW, true, true);

        assertTrue(AssignmentAutoAcceptancePolicy.isEligible(assignment, NOW));
    }

    @Test
    void futureAssignmentIsNotEligible() {
        Assignment assignment = assignment(AssignmentState.PLANNED, NOW.plusMinutes(1), true, true);

        assertFalse(AssignmentAutoAcceptancePolicy.isEligible(assignment, NOW));
    }

    @Test
    void acknowledgedDeclinedAndCancelledAssignmentsAreNotEligible() {
        assertFalse(AssignmentAutoAcceptancePolicy.isEligible(
                assignment(AssignmentState.ACKNOWLEDGED, NOW, true, true), NOW));
        assertFalse(AssignmentAutoAcceptancePolicy.isEligible(
                assignment(AssignmentState.DECLINED, NOW, true, true), NOW));
        assertFalse(AssignmentAutoAcceptancePolicy.isEligible(
                assignment(AssignmentState.CANCELLED, NOW, true, true), NOW));
    }

    @Test
    void inactiveOrUnassignedWorkIsNotEligible() {
        assertFalse(AssignmentAutoAcceptancePolicy.isEligible(
                assignment(AssignmentState.PLANNED, NOW, false, true), NOW));
        assertFalse(AssignmentAutoAcceptancePolicy.isEligible(
                assignment(AssignmentState.PLANNED, NOW, true, false), NOW));
    }

    private static Assignment assignment(AssignmentState state, LocalDateTime startTime,
                                         boolean isActive, boolean isAssigned) {
        Assignment assignment = new Assignment();
        assignment.setState(state);
        assignment.setStartTime(startTime);
        assignment.setActive(isActive);
        if (isAssigned) {
            User employee = new User();
            employee.setId(20L);
            assignment.setAssignedEmployee(employee);
        }
        return assignment;
    }
}

package app.services.entityServices;

import app.entities.Assignment;
import app.entities.AssignmentState;

import java.time.LocalDateTime;

final class AssignmentAutoAcceptancePolicy {

    private AssignmentAutoAcceptancePolicy() {
    }

    static boolean isEligible(Assignment assignment, LocalDateTime now) {
        return assignment != null
                && assignment.isActive()
                && assignment.getAssignedEmployeeId() != null
                && assignment.getState() == AssignmentState.PLANNED
                && assignment.getStartTime() != null
                && !assignment.getStartTime().isAfter(now);
    }
}

package app.services.entityServices;

import app.entities.Assignment;
import app.entities.AssignmentState;
import app.exceptions.ApiException;

import java.time.LocalDateTime;
import java.util.Objects;

final class AssignmentAcknowledgementPolicy {

    private AssignmentAcknowledgementPolicy() {
    }

    static void validate(Assignment assignment, Long tenantId, Long employeeId, LocalDateTime now) {
        if (assignment == null || !Objects.equals(assignment.getTenantId(), tenantId) || !assignment.isActive()) {
            throw new ApiException(404, "Assignment not found");
        }
        if (!Objects.equals(assignment.getAssignedEmployeeId(), employeeId)) {
            throw new ApiException(404, "Assignment not found");
        }
        if (assignment.getState() != AssignmentState.PLANNED) {
            throw new ApiException(409, "Only a planned assignment can be acknowledged");
        }
        if (assignment.getStartTime() == null) {
            throw new ApiException(409, "Assignment must have a start time before it can be acknowledged");
        }
        if (!now.isBefore(assignment.getStartTime())) {
            throw new ApiException(409, "Assignment cannot be acknowledged after it has started");
        }
    }
}

package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dto.AssignmentDTO;
import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.AssignmentStateHistory;
import app.exceptions.ApiException;
import app.services.dtoConverter.AssignmentMapper;
import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

public class AssignmentAcknowledgementService {

    private final AssignmentDAO assignmentDAO;
    private final AssignmentMapper assignmentMapper = new AssignmentMapper();
    private final Clock clock;

    public AssignmentAcknowledgementService(EntityManagerFactory entityManagerFactory) {
        this(entityManagerFactory, Clock.systemUTC());
    }

    AssignmentAcknowledgementService(EntityManagerFactory entityManagerFactory, Clock clock) {
        if (entityManagerFactory == null) {
            throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        }
        this.assignmentDAO = new AssignmentDAO(entityManagerFactory);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public AssignmentDTO acknowledge(Long assignmentId, Long tenantId, Long employeeId) {
        if (assignmentId == null) {
            throw new ApiException(400, "Assignment id is required");
        }

        Assignment assignment = assignmentDAO.getById(assignmentId);
        AssignmentAcknowledgementPolicy.validate(
                assignment,
                tenantId,
                employeeId,
                LocalDateTime.now(clock)
        );

        Instant acknowledgedAt = Instant.now(clock);
        assignment.setState(AssignmentState.ACKNOWLEDGED);
        AssignmentStateHistory history = new AssignmentStateHistory(
                assignment.getId(),
                AssignmentState.PLANNED,
                AssignmentState.ACKNOWLEDGED,
                "employee:" + employeeId,
                acknowledgedAt
        );
        return assignmentMapper.toDto(assignmentDAO.update(assignment, history));
    }
}

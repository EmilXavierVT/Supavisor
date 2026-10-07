package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.AssignmentDelegationDAO;
import app.dao.UserDAO;
import app.dto.AssignmentDelegationDTO;
import app.entities.Assignment;
import app.entities.AssignmentHistory;
import app.entities.AssignmentState;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.AssignmentAlreadyDelegatedException;
import app.services.dtoConverter.AssignmentMapper;
import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AssignmentDelegationService {

    // work that has not started yet and has not been closed
    private static final Set<AssignmentState> DELEGABLE_STATES = EnumSet.of(
            AssignmentState.PLANNED, AssignmentState.ACKNOWLEDGED, AssignmentState.AUTO_ACCEPTED);

    private final AssignmentDAO assignmentDAO;
    private final AssignmentDelegationDAO delegationDAO;
    private final UserDAO userDAO;
    private final AssignmentMapper mapper = new AssignmentMapper();
    private final Clock clock;

    public AssignmentDelegationService(EntityManagerFactory emf) {
        this(emf, Clock.systemUTC());
    }

    AssignmentDelegationService(EntityManagerFactory emf, Clock clock) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.assignmentDAO = new AssignmentDAO(emf);
        this.delegationDAO = new AssignmentDelegationDAO(emf);
        this.userDAO = new UserDAO(emf);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public List<AssignmentDelegationDTO> getDelegations(Long assignmentId, Long tenantId) {
        Assignment assignment = find(assignmentId, tenantId);
        return delegationDAO.findByAssignmentId(assignment.getId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    public void delegate(Long assignmentId, Long employeeId, Long tenantId, Long adminId, String adminSource) {
        if (employeeId == null) {
            throw new ApiException(400, "Employee is required");
        }
        Assignment assignment = find(assignmentId, tenantId);
        if (!assignment.isActive()) {
            throw new ApiException(409, "Employees cannot be assigned to an inactive assignment");
        }
        if (!DELEGABLE_STATES.contains(assignment.getState())) {
            throw new ApiException(409, "Employees can only be assigned to planned assignments. This assignment is "
                    + assignment.getState());
        }
        User employee = activeEmployee(employeeId, tenantId);
        if (Objects.equals(employeeId, assignment.getAssignedEmployeeId())
                || delegationDAO.isDelegated(assignment.getId(), employeeId)) {
            throw new ApiException(409, "Employee is already assigned to this assignment");
        }
        rejectOverlaps(assignment, employeeId);

        String source = validSource(adminSource);
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        AssignmentHistory history = new AssignmentHistory(assignment.getId(), "DELEGATE", null, employeeId,
                source, now, "Assigned employee " + employeeId + " (" + displayName(employee) + ")");
        try {
            delegationDAO.create(assignment.getId(), employeeId, adminId, source, now, history);
        } catch (AssignmentAlreadyDelegatedException e) {
            throw new ApiException(409, "Employee is already assigned to this assignment");
        }
    }

    public void undelegate(Long assignmentId, Long employeeId, Long tenantId, String adminSource) {
        Assignment assignment = find(assignmentId, tenantId);
        String source = validSource(adminSource);
        AssignmentHistory history = new AssignmentHistory(assignment.getId(), "UNDELEGATE", employeeId, null,
                source, Instant.now(clock).truncatedTo(ChronoUnit.MICROS),
                "Removed assigned employee " + employeeId);
        if (!delegationDAO.delete(assignment.getId(), employeeId, history)) {
            throw new ApiException(404, "Employee is not assigned to this assignment");
        }
    }

    private Assignment find(Long id, Long tenantId) {
        Assignment assignment = id == null ? null : assignmentDAO.getById(id);
        if (assignment == null || !assignment.getTenantId().equals(tenantId)) {
            throw new ApiException(404, "Assignment not found");
        }
        return assignment;
    }

    private User activeEmployee(Long employeeId, Long tenantId) {
        User employee = userDAO.getById(employeeId);
        if (employee == null || !tenantId.equals(employee.getTenantId())) {
            throw new ApiException(400, "Employee not found");
        }
        if (!employee.getIsActive()) {
            throw new ApiException(409, "Employee is deactivated and cannot be assigned");
        }
        return employee;
    }

    private void rejectOverlaps(Assignment assignment, Long employeeId) {
        List<Assignment> conflicts = assignmentDAO.findOverlappingAssignments(
                assignment.getTenantId(),
                employeeId,
                assignment.getStartTime(),
                assignment.getEstimatedEndTime(),
                assignment.getId());
        if (!conflicts.isEmpty()) {
            String details = conflicts.stream()
                    .map(conflict -> conflict.getId() + " " + conflict.getName()
                            + " " + conflict.getStartTime() + "-" + conflict.getEstimatedEndTime())
                    .collect(Collectors.joining("; "));
            throw new ApiException(409, "Employee already has overlapping assignments: " + details);
        }
    }

    private static String displayName(User employee) {
        return employee.getName() == null || employee.getName().isBlank() ? employee.getEmail() : employee.getName();
    }

    private static String validSource(String source) {
        String trimmed = source == null ? "" : source.trim();
        return trimmed.isEmpty() ? "system" : trimmed;
    }
}

package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.AssignmentHistoryDAO;
import app.entities.Assignment;
import app.entities.AssignmentHistory;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.time.Instant;
import java.util.List;

public class AssignmentService {

    private final AssignmentDAO assignmentDAO;
    private final AssignmentHistoryDAO historyDAO;

    public AssignmentService(EntityManagerFactory emf) {
        if (emf == null) {
            throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        }
        this.assignmentDAO = new AssignmentDAO(emf);
        this.historyDAO = new AssignmentHistoryDAO(emf);
    }

    public AssignmentService(AssignmentDAO assignmentDAO, AssignmentHistoryDAO historyDAO) {
        this.assignmentDAO = assignmentDAO;
        this.historyDAO = historyDAO;
    }

    public List<Assignment> getAll(Long tenantId, boolean activeOnly) {
        return assignmentDAO.getAll(tenantId, activeOnly);
    }

    public Assignment getById(Long id, Long tenantId) {
        Assignment assignment = assignmentDAO.getById(id);
        if (assignment == null || !assignment.getTenantId().equals(tenantId)) {
            throw new ApiException(404, "Assignment not found");
        }
        return assignment;
    }

    public Assignment create(Assignment assignment, String changedBy) {
        // If your DAO create method takes (assignment, userId), adjust parameters accordingly
        Assignment created = assignmentDAO.getById(assignment.getId());
        if (created == null) {
            // Standard fallback if creating directly through DAO
            created = assignment;
        }

        saveAudit(
                assignment.getId(),
                "CREATE",
                null,
                assignment.getAssignedEmployeeId(),
                changedBy,
                "Assignment created"
        );

        return assignment;
    }

    public Assignment update(Assignment assignment, String changedBy) {
        Assignment existing = assignmentDAO.getById(assignment.getId());
        if (existing == null || !existing.getTenantId().equals(assignment.getTenantId())) {
            throw new ApiException(404, "Assignment not found");
        }

        Long previousEmployeeId = existing.getAssignedEmployeeId();
        Assignment updated = assignmentDAO.update(assignment, changedBy != null ? Long.parseLong(changedBy) : 0L);

        saveAudit(
                updated.getId(),
                "UPDATE",
                previousEmployeeId,
                updated.getAssignedEmployeeId(),
                changedBy,
                "Assignment details updated"
        );

        return updated;
    }

    public void delete(Long id, Long tenantId, String changedBy) {
        Assignment existing = getById(id, tenantId);

        Long userId = changedBy != null ? Long.parseLong(changedBy) : 0L;
        assignmentDAO.delete(id, userId);

        saveAudit(
                id,
                "DELETE",
                existing.getAssignedEmployeeId(),
                null,
                changedBy,
                "Assignment deleted"
        );
    }

    public Assignment reassign(Long assignmentId, Long newEmployeeId, Long tenantId, String changedBy) {
        Assignment existing = getById(assignmentId, tenantId);
        Long previousEmployeeId = existing.getAssignedEmployeeId();

        try {
            Long userId = changedBy != null ? Long.parseLong(changedBy) : 0L;
            Assignment updated = assignmentDAO.reassign(assignmentId, newEmployeeId, userId);

            saveAudit(
                    assignmentId,
                    "REASSIGN",
                    previousEmployeeId,
                    newEmployeeId,
                    changedBy,
                    "Reassigned employee to ID " + newEmployeeId
            );

            return updated;
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public Assignment cancel(Long assignmentId, Long tenantId, String changedBy) {
        Assignment existing = getById(assignmentId, tenantId);

        try {
            Assignment cancelled = assignmentDAO.cancel(assignmentId);

            saveAudit(
                    assignmentId,
                    "CANCEL",
                    cancelled.getAssignedEmployeeId(),
                    cancelled.getAssignedEmployeeId(),
                    changedBy,
                    "Assignment unflagged and cancelled"
            );

            return cancelled;
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public Assignment removeEmployee(Long assignmentId, Long tenantId, String changedBy) {
        Assignment existing = getById(assignmentId, tenantId);
        Long previousEmployeeId = existing.getAssignedEmployeeId();

        if (previousEmployeeId == null) {
            throw new ApiException(400, "Assignment does not currently have an assigned employee");
        }

        existing.setAssignedEmployeeId(null);
        Long userId = changedBy != null ? Long.parseLong(changedBy) : 0L;
        Assignment updated = assignmentDAO.update(existing, userId);

        saveAudit(
                assignmentId,
                "REMOVE_EMPLOYEE",
                previousEmployeeId,
                null,
                changedBy,
                "Removed employee ID " + previousEmployeeId + " from assignment"
        );

        return updated;
    }

    public List<AssignmentHistory> getHistory(Long assignmentId, Long tenantId) {
        getById(assignmentId, tenantId); // Validates existence and tenant access
        return historyDAO.findByAssignmentId(assignmentId);
    }

    private void saveAudit(Long assignmentId, String action, Long prevEmp, Long newEmp, String changedBy, String details) {
        AssignmentHistory history = new AssignmentHistory(
                assignmentId,
                action,
                prevEmp,
                newEmp,
                changedBy != null ? changedBy : "SYSTEM",
                Instant.now(),
                details
        );
        // Using create() for standalone service auditing
        historyDAO.create(history);
    }
}
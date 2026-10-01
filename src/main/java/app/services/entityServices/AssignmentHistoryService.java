package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.AssignmentHistoryDAO;
import app.entities.Assignment;
import app.entities.AssignmentHistory;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Service dedicated to managing and querying assignment audit history.
 * Ensures all history lookups are properly tenant-scoped.
 */
public class AssignmentHistoryService {

    private final AssignmentHistoryDAO historyDAO;
    private final AssignmentDAO assignmentDAO;

    public AssignmentHistoryService(EntityManagerFactory emf) {
        if (emf == null) {
            throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        }
        this.historyDAO = new AssignmentHistoryDAO(emf);
        this.assignmentDAO = new AssignmentDAO(emf);
    }

    public AssignmentHistoryService(AssignmentHistoryDAO historyDAO, AssignmentDAO assignmentDAO) {
        this.historyDAO = historyDAO;
        this.assignmentDAO = assignmentDAO;
    }

    /**
     * Retrieves the audit log history for a specific assignment.
     * Verifies that the assignment exists and belongs to the calling tenant.
     */
    public List<AssignmentHistory> getHistoryForAssignment(Long assignmentId, Long tenantId) {
        verifyAssignmentAccess(assignmentId, tenantId);
        return historyDAO.findByAssignmentId(assignmentId);
    }

    /**
     * Creates and records an audit log entry using a standalone transaction.
     */
    public AssignmentHistory logChange(Long assignmentId, String action, Long previousEmployeeId,
                                       Long newEmployeeId, String changedBy, String details) {
        AssignmentHistory history = new AssignmentHistory(
                assignmentId,
                action,
                previousEmployeeId,
                newEmployeeId,
                changedBy != null ? changedBy : "SYSTEM",
                Instant.now(),
                details
        );

        // Uses create() for standalone persistence outside an active transaction
        return historyDAO.create(history);
    }

    private void verifyAssignmentAccess(Long assignmentId, Long tenantId) {
        if (assignmentId == null) {
            throw new ApiException(400, "Assignment ID is required");
        }
        Assignment assignment = assignmentDAO.getById(assignmentId);
        if (assignment == null || !assignment.getTenantId().equals(tenantId)) {
            throw new ApiException(404, "Assignment not found");
        }
    }
}
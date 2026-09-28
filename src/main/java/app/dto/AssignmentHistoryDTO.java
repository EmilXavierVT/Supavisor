package app.dto;

import app.entities.AssignmentHistory;
import java.time.Instant;

public class AssignmentHistoryDTO {

    private Long id;
    private Long assignmentId;
    private String action;
    private Long preEmployee;
    private Long newEmployee;
    private String changedBy;
    private Instant timestamp;
    private String details;

    public AssignmentHistoryDTO() {}

    public AssignmentHistoryDTO(AssignmentHistory entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.assignmentId = entity.getAssignmentId();
            this.action = entity.getAction();
            this.preEmployee = entity.getPreEmployee();
            this.newEmployee = entity.getNewEmployee();
            this.changedBy = entity.getChangedBy();
            this.timestamp = entity.getTimestamp();
            this.details = entity.getDetails();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getPreEmployee() { return preEmployee; }
    public void setPreEmployee(Long preEmployee) { this.preEmployee = preEmployee; }

    public Long getNewEmployee() { return newEmployee; }
    public void setNewEmployee(Long newEmployee) { this.newEmployee = newEmployee; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
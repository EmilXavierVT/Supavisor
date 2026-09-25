package app.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assignment_history")
public class AssignmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(nullable = false)
    private String action;

    @Column(name = "previous_employee_id")
    private Long previousEmployeeId;

    @Column(name = "new_employee_id")
    private Long newEmployeeId;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(length = 1000)
    private String details;

    public AssignmentHistory() {}

    // 7-parameter constructor matching saveAudit
    public AssignmentHistory(Long assignmentId, String action, Long previousEmployeeId, Long newEmployeeId, String changedBy, Instant changedAt, String details) {
        this.assignmentId = assignmentId;
        this.action = action;
        this.previousEmployeeId = previousEmployeeId;
        this.newEmployeeId = newEmployeeId;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getPreviousEmployeeId() { return previousEmployeeId; }
    public void setPreviousEmployeeId(Long previousEmployeeId) { this.previousEmployeeId = previousEmployeeId; }

    public Long getNewEmployeeId() { return newEmployeeId; }
    public void setNewEmployeeId(Long newEmployeeId) { this.newEmployeeId = newEmployeeId; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
package app.dto;

import app.entities.ProjectStatus;

import java.time.Instant;

public class ProjectStatusHistoryDTO {

    private Long id;
    private Long projectId;
    private ProjectStatus fromStatus;
    private ProjectStatus toStatus;
    private String changedBy;
    private Instant changedAt;

    public ProjectStatusHistoryDTO() {}

    public ProjectStatusHistoryDTO(Long id, Long projectId, ProjectStatus fromStatus, ProjectStatus toStatus, String changedBy, Instant changedAt) {
        this.id = id;
        this.projectId = projectId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public ProjectStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(ProjectStatus fromStatus) { this.fromStatus = fromStatus; }

    public ProjectStatus getToStatus() { return toStatus; }
    public void setToStatus(ProjectStatus toStatus) { this.toStatus = toStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}

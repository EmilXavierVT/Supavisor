package app.dto;

import app.entities.ProjectStatus;

import java.util.ArrayList;
import java.util.List;

public class ProjectDTO {

    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private List<Long> assignmentIds = new ArrayList<>();
    private ProjectStatus status;

    public ProjectDTO() {}

    public ProjectDTO(Long id, Long tenantId, String name, String description, List<Long> assignmentIds, ProjectStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        setAssignmentIds(assignmentIds);
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // output only: the tenant always comes from the caller's token, never from the request body
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Long> getAssignmentIds() { return assignmentIds; }
    public void setAssignmentIds(List<Long> assignmentIds) {
        this.assignmentIds = assignmentIds == null ? new ArrayList<>() : new ArrayList<>(assignmentIds);
    }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
}

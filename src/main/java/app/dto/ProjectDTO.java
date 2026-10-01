package app.dto;

import app.entities.ProjectStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {

    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private Long customerId;
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<Long> assignmentIds = new ArrayList<>();
    private ProjectStatus status;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    public ProjectDTO(Long id, Long tenantId, String name, String description, List<Long> assignmentIds, ProjectStatus status) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        setAssignmentIds(assignmentIds);
        this.status = status;
    }

    public void setAssignmentIds(List<Long> assignmentIds) {
        this.assignmentIds = assignmentIds == null ? new ArrayList<>() : new ArrayList<>(assignmentIds);
    }
}

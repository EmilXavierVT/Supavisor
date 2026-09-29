package app.dto;

import app.entities.ProjectStatus;

import java.time.Instant;
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
public class ProjectStatusHistoryDTO {

    private Long id;
    private Long projectId;
    private ProjectStatus fromStatus;
    private ProjectStatus toStatus;
    private String changedBy;
    private Instant changedAt;
}

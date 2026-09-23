package app.services.dtoConverter;

import app.dto.ProjectDTO;
import app.dto.ProjectStatusHistoryDTO;
import app.entities.Project;
import app.entities.ProjectStatusHistory;

public class ProjectMapper {

    public ProjectDTO toDto(Project entity) {
        if (entity == null) return null;
        ProjectDTO dto = new ProjectDTO(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAssignmentIds(),
                entity.getStatus()
        );
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public ProjectStatusHistoryDTO toDto(ProjectStatusHistory entity) {
        if (entity == null) return null;
        return new ProjectStatusHistoryDTO(
                entity.getId(),
                entity.getProjectId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getChangedBy(),
                entity.getChangedAt()
        );
    }
}

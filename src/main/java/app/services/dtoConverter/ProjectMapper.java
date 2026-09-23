package app.services.dtoConverter;

import app.dto.ProjectDTO;
import app.entities.Project;

public class ProjectMapper {

    public ProjectDTO toDto(Project entity) {
        if (entity == null) return null;
        return new ProjectDTO(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAssignmentIds(),
                entity.getStatus()
        );
    }
}

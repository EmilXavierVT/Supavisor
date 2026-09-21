package app.services.dtoConverter;

import app.dto.AssignmentDTO;
import app.entities.Assignment;

public class AssignmentMapper {

    public AssignmentDTO toDto(Assignment entity) {
        if (entity == null) return null;
        return new AssignmentDTO(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                entity.isActive()
        );
    }
}

package app.services.dtoConverter;

import app.dto.AssignmentDTO;
import app.entities.Assignment;

public class AssignmentMapper {

    public AssignmentDTO toDto(Assignment entity) {
        if (entity == null) return null;
        AssignmentDTO dto = new AssignmentDTO(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                entity.isActive()
        );
        dto.setIsFlagged(entity.isFlagged());
        dto.setAddress(entity.getAddress());
        dto.setEstimatedMinutes(entity.getEstimatedMinutes());
        dto.setCost(entity.getCost());
        dto.setAssignedEmployeeId(entity.getAssignedEmployeeId());
        dto.setMissingEmployeeCount(entity.getMissingEmployeeCount());
        return dto;
    }
}
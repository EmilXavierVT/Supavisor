package app.services.dtoConverter;

import app.dto.AssignmentDTO;
import app.dto.AssignmentStateHistoryDTO;
import app.entities.Assignment;
import app.entities.AssignmentStateHistory;
import app.entities.User;

public class AssignmentMapper {

    public AssignmentDTO toDto(Assignment entity) {
        if (entity == null) return null;
        AssignmentDTO dto = new AssignmentDTO(
                entity.getId(),
                entity.getName(),
                entity.getTenantId(),
                entity.isActive()
        );
        dto.setAddress(entity.getAddress());
        dto.setEstimatedMinutes(entity.getEstimatedMinutes());
        dto.setCost(entity.getCost());
        dto.setStartTime(entity.getStartTime());
        dto.setEstimatedEndTime(entity.getEstimatedEndTime());
        dto.setState(entity.getState());
        dto.setCheckInAt(entity.getCheckInAt());
        dto.setCheckOutAt(entity.getCheckOutAt());
        User employee = entity.getAssignedEmployee();
        dto.setAssignedEmployeeId(employee == null ? null : employee.getId());
        dto.setAssignedEmployeeName(employee == null ? null : employee.getName());
        dto.setProductIds(entity.getProductIds());
        return dto;
    }

    public AssignmentStateHistoryDTO toDto(AssignmentStateHistory entity) {
        if (entity == null) return null;
        return new AssignmentStateHistoryDTO(
                entity.getId(),
                entity.getAssignmentId(),
                entity.getFromState(),
                entity.getToState(),
                entity.getSource(),
                entity.getChangedAt()
        );
    }
}

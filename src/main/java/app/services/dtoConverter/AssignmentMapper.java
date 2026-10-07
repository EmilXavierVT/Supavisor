package app.services.dtoConverter;

import app.dto.AssignmentDTO;
import app.dto.AssignmentAuditHistoryDTO;
import app.dto.AssignmentResourceRequirementDTO;
import app.dto.AssignmentStateHistoryDTO;
import app.entities.Assignment;
import app.entities.AssignmentAuditHistory;
import app.entities.AssignmentResourceRequirement;
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
        dto.setVersion(entity.getVersion());
        dto.setAddress(entity.getAddress());
        dto.setIsFlagged(entity.isFlagged());
        dto.setMissingEmployeeCount(entity.getMissingEmployeeCount());
        dto.setNotes(entity.getNotes());
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
        dto.setAssignedEmployeeQualifications(employee == null ? java.util.Set.of() : employee.getQualifications().stream()
                .map(new QualificationMapper()::toDTO)
                .collect(java.util.stream.Collectors.toSet()));
        dto.setProductIds(entity.getProductIds());
        dto.setResourceRequirements(entity.getResourceRequirements().stream()
                .map(this::toDto)
                .toList());
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

    public AssignmentAuditHistoryDTO toDto(AssignmentAuditHistory entity) {
        if (entity == null) return null;
        return new AssignmentAuditHistoryDTO(
                entity.getId(),
                entity.getAssignmentId(),
                entity.getAuditType(),
                entity.getActorUserId(),
                entity.getActorSource(),
                entity.getReason(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }

    private AssignmentResourceRequirementDTO toDto(AssignmentResourceRequirement entity) {
        return new AssignmentResourceRequirementDTO(entity.getProductId(), entity.getMode());
    }
}

package app.services.dtoConverter;

import app.dto.AssignmentTypeDTO;
import app.entities.AssignmentType;
import jakarta.persistence.EntityManagerFactory;

public class AssignmentTypeMapper {

    public AssignmentTypeMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
    }

    public AssignmentTypeDTO toDto(AssignmentType entity) {
        if (entity == null) return null;
        return new AssignmentTypeDTO(
                entity.getId(),
                entity.getName(),
                entity.isActive()
        );
    }

    public AssignmentType fromDto(AssignmentTypeDTO dto) {
        if (dto == null) return null;
        AssignmentType entity = new AssignmentType();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        if (dto.getIsActive() != null) {
            entity.setActive(dto.getIsActive());
        }
        return entity;
    }
}
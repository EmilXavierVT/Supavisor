package app.services.dtoConverter;

import app.dto.QualificationDTO;
import app.entities.Qualification;

public class QualificationMapper {
    public QualificationDTO toDTO(Qualification qualification) {
        if (qualification == null) return null;
        return new QualificationDTO(
                qualification.getId(),
                qualification.getTenantId(),
                qualification.getName()
        );
    }

    public Qualification toEntity(QualificationDTO dto, Long tenantId) {
        if (dto == null) return null;
        return Qualification.builder()
                .id(dto.getId())
                .tenantId(tenantId)
                .name(dto.getName())
                .build();
    }
}

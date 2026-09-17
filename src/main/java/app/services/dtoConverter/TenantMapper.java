package app.services.dtoConverter;

import app.dto.TenantDTO;
import app.entities.Tenant;
import jakarta.persistence.EntityManagerFactory;

public class TenantMapper {
    public TenantMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
    }

    public TenantDTO toDto(Tenant tenant) {
        if (tenant == null) return null;
        return new TenantDTO(tenant.getId(), tenant.getName());
    }

    public Tenant fromDto(TenantDTO dto) {
        if (dto == null) return null;
        return new Tenant(dto.getId(), dto.getName());
    }
}

package app.services.dtoConverter;

import app.dto.RoleDTO;
import app.entities.Role;
import app.entities.Tenant;
import java.util.Objects;

public class RoleMapper {
    public RoleDTO toDTO(Role role) {
        if (role == null) return null;
        return RoleDTO.builder()
                .id(role.getId())
                .roleName(role.getRoleName())
                .tenantId(role.getTenant() != null ? role.getTenant().getId() : null)
                .build();
    }

    public Role toEntity(RoleDTO dto, Tenant tenant) {
        if (dto == null) return null;
        Role role = new Role();
        role.setId(dto.getId());
        role.setRoleName(dto.getRoleName());
        role.setTenant(tenant);
        return role;
    }
}

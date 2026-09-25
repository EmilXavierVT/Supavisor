package app.services.dtoConverter;

import app.dto.RoleDTO;
import app.entities.Role;
import app.entities.Tenant;

public class RoleMapper {
    public RoleDTO toDTO(Role role) {
        if (role == null) return null;
        return new RoleDTO(
                role.getId(),
                role.getRoleName(),
                role.getTenant() != null ? role.getTenant().getId() : null
        );
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

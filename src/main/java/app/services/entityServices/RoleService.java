package app.services.entityServices;

import app.dao.IRoleDAO;
import app.exceptions.ApiException;
import app.entities.Role;
import app.entities.Tenant;
import app.services.dtoConverter.RoleMapper;
import app.dto.RoleDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RoleService {
    private final IRoleDAO roleDAO;
    private final RoleMapper roleMapper;
    private final EntityManager entityManager;

    public RoleService(EntityManagerFactory emf) {
        this.roleDAO = new app.dao.RoleDAO(emf);
        this.roleMapper = new RoleMapper();
        this.entityManager = emf.createEntityManager();
    }

    public List<RoleDTO> getRolesByTenant(Long tenantId) {
        return roleDAO.findByTenantId(tenantId).stream()
                .map(roleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public RoleDTO createRole(RoleDTO roleDTO, Long tenantId) {
        String roleName = roleDTO.getRoleName() == null ? "" : roleDTO.getRoleName().trim();
        if (roleName.isEmpty()) {
            throw new ApiException(400, "Role name is required");
        }
        if (roleName.length() > 255) {
            throw new ApiException(400, "Role name must be at most 255 characters");
        }
        roleDTO.setRoleName(roleName);

        Tenant tenant = entityManager.getReference(Tenant.class, tenantId);
        
        // Check uniqueness
        boolean exists = roleDAO.findByTenantId(tenantId).stream()
                .anyMatch(r -> r.getRoleName().equalsIgnoreCase(roleName));
        
        if (exists) {
            throw new ApiException(409, "A role with this name already exists");
        }

        Role role = roleMapper.toEntity(roleDTO, tenant);
        Role savedRole = roleDAO.save(role);
        return roleMapper.toDTO(savedRole);
    }

    public void deleteRole(Long roleId, Long tenantId) {
        roleDAO.findById(roleId).ifPresent(role -> {
            if (role.getTenant().getId().equals(tenantId)) {
                roleDAO.delete(roleId);
            } else {
                throw new ApiException(403, "Role does not belong to this tenant");
            }
        });
    }
}

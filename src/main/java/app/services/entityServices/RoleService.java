package app.services.entityServices;

import app.dao.IRoleDAO;
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
        Tenant tenant = entityManager.getReference(Tenant.class, tenantId);
        
        // Check uniqueness
        boolean exists = roleDAO.findByTenantId(tenantId).stream()
                .anyMatch(r -> r.getRoleName().equalsIgnoreCase(roleDTO.getRoleName()));
        
        if (exists) {
            throw new RuntimeException("Role name already exists for this tenant");
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
                throw new RuntimeException("Role does not belong to this tenant");
            }
        });
    }
}

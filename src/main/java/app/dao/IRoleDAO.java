package app.dao;

import app.entities.Role;
import java.util.List;
import java.util.Optional;

public interface IRoleDAO {
    Optional<Role> findById(Long id);
    List<Role> findByTenantId(Long tenantId);
    Role save(Role role);
    void delete(Long id);
}

package app.services.entityServices;

import app.dao.EmployeeCategoryDAO;
import app.dto.EmployeeCategoryDTO;
import app.entities.EmployeeCategory;
import app.exceptions.ApiException;
import app.services.dtoConverter.EmployeeCategoryMapper;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class EmployeeCategoryService {
    private static final int MAX_NAME_LENGTH = 100;

    private final EmployeeCategoryDAO dao;
    private final EmployeeCategoryMapper mapper = new EmployeeCategoryMapper();

    public EmployeeCategoryService(EntityManagerFactory emf) {
        this.dao = new EmployeeCategoryDAO(emf);
    }

    public List<EmployeeCategoryDTO> getAll(Long tenantId, boolean activeOnly) {
        return dao.getAll(tenantId, activeOnly).stream().map(mapper::toDto).toList();
    }

    public EmployeeCategoryDTO create(EmployeeCategoryDTO dto, Long tenantId) {
        EmployeeCategory category = new EmployeeCategory();
        category.setName(validName(dto.getName()));
        category.setTenantId(tenantId);
        category.setActive(dto.getActive() == null || dto.getActive());
        return mapper.toDto(dao.create(category));
    }

    public EmployeeCategoryDTO setActive(Long id, Long tenantId, boolean active) {
        EmployeeCategory category = find(id, tenantId);
        category.setActive(active);
        return mapper.toDto(dao.update(category));
    }

    private EmployeeCategory find(Long id, Long tenantId) {
        EmployeeCategory category = id == null ? null : dao.getById(id);
        if (category == null || !tenantId.equals(category.getTenantId())) {
            throw new ApiException(404, "Employee category not found");
        }
        return category;
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(400, "Employee category name is required");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(400, "Employee category name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Employee category name contains invalid characters");
        }
        return trimmed;
    }
}

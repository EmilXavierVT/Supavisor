package app.services.dtoConverter;

import app.dto.EmployeeCategoryDTO;
import app.entities.EmployeeCategory;

public class EmployeeCategoryMapper {
    public EmployeeCategoryDTO toDto(EmployeeCategory category) {
        if (category == null) return null;
        return new EmployeeCategoryDTO(category.getId(), category.getName(), category.getTenantId(), category.isActive());
    }
}

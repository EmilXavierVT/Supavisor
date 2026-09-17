package app.services.entityServices;

import app.dao.AssignmentTypeDAO;
import app.dto.AssignmentTypeDTO;
import app.entities.AssignmentType;
import app.services.dtoConverter.AssignmentTypeMapper;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class AssignmentTypeService {

    private final AssignmentTypeDAO dao;
    private final AssignmentTypeMapper mapper;

    public AssignmentTypeService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.dao = new AssignmentTypeDAO(emf);
        this.mapper = new AssignmentTypeMapper(emf);
    }

    public List<AssignmentTypeDTO> getAllAssignmentTypes(boolean activeOnly) {
        List<AssignmentType> list = dao.getAll(activeOnly);
        return list.stream()
                .map(mapper::toDto)
                .toList();
    }

    public AssignmentTypeDTO createAssignmentType(AssignmentTypeDTO dto) {
        AssignmentType entity = mapper.fromDto(dto);
        AssignmentType created = dao.create(entity);
        return mapper.toDto(created);
    }

    public AssignmentTypeDTO updateAssignmentType(Integer id, AssignmentTypeDTO dto) {
        AssignmentType existing = dao.getById(id);
        if (existing == null) {
            return null;
        }

        existing.setName(dto.getName());
        if (dto.getIsActive() != null) {
            existing.setActive(dto.getIsActive());
        }

        AssignmentType updated = dao.update(existing);
        return mapper.toDto(updated);
    }

    public void deleteAssignmentType(Integer id) {
        dao.delete(id);
    }

    public void deactivateAssignmentType(Integer id) {
        AssignmentType existing = dao.getById(id);
        if (existing != null) {
            existing.setActive(false);
            dao.update(existing);
        }
    }
}
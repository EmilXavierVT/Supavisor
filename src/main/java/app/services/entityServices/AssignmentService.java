package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dto.AssignmentDTO;
import app.entities.Assignment;
import app.exceptions.ApiException;
import app.exceptions.AssignmentInUseException;
import app.services.dtoConverter.AssignmentMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.List;

/**
 * Everything is scoped to a tenant: a tenant only ever sees and touches its own assignments,
 * and an assignment of another tenant is reported as not found.
 */
public class AssignmentService {

    private static final int MAX_NAME_LENGTH = 100;

    private final AssignmentDAO dao;
    private final AssignmentMapper mapper = new AssignmentMapper();

    public AssignmentService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.dao = new AssignmentDAO(emf);
    }

    /** @param activeOnly true for the list a picker should offer: deactivated assignments are left out */
    public List<AssignmentDTO> getAll(Long tenantId, boolean activeOnly) {
        return dao.getAll(tenantId, activeOnly).stream()
                .map(mapper::toDto)
                .toList();
    }

    /** @param activeOnly when true a deactivated assignment is reported as not found */
    public AssignmentDTO getById(Long id, Long tenantId, boolean activeOnly) {
        Assignment assignment = find(id, tenantId);
        if (activeOnly && !assignment.isActive()) {
            throw notFound();
        }
        return mapper.toDto(assignment);
    }

    public AssignmentDTO create(AssignmentDTO dto, Long tenantId) {
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, null);

        Assignment assignment = new Assignment();
        assignment.setName(name);
        assignment.setTenantId(tenantId);
        assignment.setActive(dto.getIsActive() == null || dto.getIsActive());
        try {
            return mapper.toDto(dao.create(assignment));
        } catch (PersistenceException e) {
            // lost a race against another request using the same name
            rejectDuplicate(tenantId, name, null);
            throw e;
        }
    }

    /** Replaces the name; the active flag is only changed when the request carries one. */
    public AssignmentDTO update(Long id, AssignmentDTO dto, Long tenantId) {
        Assignment existing = find(id, tenantId);
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, id);

        existing.setName(name);
        if (dto.getIsActive() != null) {
            existing.setActive(dto.getIsActive());
        }
        try {
            return mapper.toDto(dao.update(existing));
        } catch (PersistenceException e) {
            rejectDuplicate(tenantId, name, id);
            throw e;
        }
    }

    /** Only flips the flag, so everything that already references the assignment keeps it. */
    public AssignmentDTO deactivate(Long id, Long tenantId) {
        Assignment existing = find(id, tenantId);
        if (existing.isActive()) {
            existing.setActive(false);
            existing = dao.update(existing);
        }
        return mapper.toDto(existing);
    }

    /** Refuses with 409 while other records still reference the assignment; deactivate it instead. */
    public void delete(Long id, Long tenantId) {
        find(id, tenantId);
        try {
            dao.delete(id);
        } catch (AssignmentInUseException e) {
            throw new ApiException(409, "This assignment is in use and cannot be deleted. Deactivate it instead");
        }
    }

    private Assignment find(Long id, Long tenantId) {
        Assignment assignment = id == null ? null : dao.getById(id);
        if (assignment == null || !assignment.getTenantId().equals(tenantId)) {
            throw notFound();
        }
        return assignment;
    }

    private static ApiException notFound() {
        return new ApiException(404, "Assignment not found");
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(400, "Assignment name is required");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(400, "Assignment name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Assignment name contains invalid characters");
        }
        return trimmed;
    }

    /** Names are unique per tenant, ignoring case. {@code ownId} is the assignment being renamed, if any. */
    private void rejectDuplicate(Long tenantId, String name, Long ownId) {
        boolean taken = dao.findByName(tenantId, name).stream()
                .anyMatch(other -> !other.getId().equals(ownId));
        if (taken) {
            throw new ApiException(409, "An assignment with this name already exists");
        }
    }
}

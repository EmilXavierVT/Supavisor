package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.UserDAO;
import app.dto.AssignmentDTO;
import app.entities.Assignment;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.AssignmentInUseException;
import app.services.dtoConverter.AssignmentMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything is scoped to a tenant: a tenant only ever sees and touches its own assignments,
 * and an assignment of another tenant is reported as not found.
 */
public class AssignmentService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_ESTIMATED_MINUTES = 525_600; // one year
    private static final BigDecimal MAX_COST = new BigDecimal("9999999999.99"); // fits numeric(12,2)

    private final AssignmentDAO dao;
    private final UserDAO userDAO;
    private final AssignmentMapper mapper = new AssignmentMapper();

    public AssignmentService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.dao = new AssignmentDAO(emf);
        this.userDAO = new UserDAO(emf);
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
        applyDetails(assignment, dto, tenantId);
        try {
            return mapper.toDto(dao.create(assignment));
        } catch (PersistenceException e) {
            // lost a race against another request using the same name
            rejectDuplicate(tenantId, name, null);
            throw e;
        }
    }

    /**
     * Replaces the name and all details (address, estimated time, cost, employee): a field left out or
     * null is cleared. The active flag is only changed when the request carries one.
     */
    public AssignmentDTO update(Long id, AssignmentDTO dto, Long tenantId) {
        Assignment existing = find(id, tenantId);
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, id);

        existing.setName(name);
        applyDetails(existing, dto, tenantId);
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
        return setActive(id, tenantId, false);
    }

    public AssignmentDTO activate(Long id, Long tenantId) {
        return setActive(id, tenantId, true);
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

    private AssignmentDTO setActive(Long id, Long tenantId, boolean active) {
        Assignment existing = find(id, tenantId);
        if (existing.isActive() != active) {
            existing.setActive(active);
            existing = dao.update(existing);
        }
        return mapper.toDto(existing);
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

    /** Validates and copies the optional details; nothing is changed on the entity if any of them is invalid. */
    private void applyDetails(Assignment assignment, AssignmentDTO dto, Long tenantId) {
        String address = validAddress(dto.getAddress());
        Integer estimatedMinutes = validEstimatedMinutes(dto.getEstimatedMinutes());
        BigDecimal cost = validCost(dto.getCost());
        Long employeeId = validEmployee(dto.getAssignedEmployeeId(), assignment.getAssignedEmployeeId(), tenantId);

        assignment.setAddress(address);
        assignment.setEstimatedMinutes(estimatedMinutes);
        assignment.setCost(cost);
        assignment.setAssignedEmployeeId(employeeId);
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

    private static String validAddress(String address) {
        String trimmed = address == null ? "" : address.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_ADDRESS_LENGTH) {
            throw new ApiException(400, "Address must be at most " + MAX_ADDRESS_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Address contains invalid characters");
        }
        return trimmed;
    }

    private static Integer validEstimatedMinutes(Integer minutes) {
        if (minutes == null) {
            return null;
        }
        if (minutes <= 0 || minutes > MAX_ESTIMATED_MINUTES) {
            throw new ApiException(400, "Estimated time must be between 1 and " + MAX_ESTIMATED_MINUTES + " minutes");
        }
        return minutes;
    }

    private static BigDecimal validCost(BigDecimal cost) {
        if (cost == null) {
            return null;
        }
        if (cost.signum() < 0) {
            throw new ApiException(400, "Cost cannot be negative");
        }
        if (cost.stripTrailingZeros().scale() > 2) {
            throw new ApiException(400, "Cost can have at most 2 decimals");
        }
        if (cost.compareTo(MAX_COST) > 0) {
            throw new ApiException(400, "Cost is too large");
        }
        return cost.setScale(2);
    }

    /**
     * The employee has to be a user of the same tenant. A deactivated employee cannot be newly assigned,
     * but one who is already linked stays linked (history is preserved) when other details are edited.
     */
    private Long validEmployee(Long employeeId, Long currentEmployeeId, Long tenantId) {
        if (employeeId == null) {
            return null;
        }
        User employee = userDAO.getById(employeeId);
        if (employee == null || !tenantId.equals(employee.getTenantId())) {
            throw new ApiException(400, "Employee not found");
        }
        if (!employee.getIsActive() && !employeeId.equals(currentEmployeeId)) {
            throw new ApiException(400, "Employee is deactivated and cannot be assigned");
        }
        return employeeId;
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

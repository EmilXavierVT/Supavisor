package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.ProductDAO;
import app.dao.UserDAO;
import app.dto.AssignmentDTO;
import app.dto.AssignmentStateHistoryDTO;
import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.AssignmentStateHistory;
import app.entities.Product;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.AssignmentInUseException;
import app.services.dtoConverter.AssignmentMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;


public class AssignmentService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_ESTIMATED_MINUTES = 525_600; // one year
    private static final BigDecimal MAX_COST = new BigDecimal("9999999999.99"); // fits numeric(12,2)

    private final AssignmentDAO dao;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final AssignmentMapper mapper = new AssignmentMapper();
    private final Clock clock;

    public AssignmentService(EntityManagerFactory emf) {
        this(emf, Clock.systemUTC());
    }

    AssignmentService(EntityManagerFactory emf, Clock clock) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.dao = new AssignmentDAO(emf);
        this.userDAO = new UserDAO(emf);
        this.productDAO = new ProductDAO(emf);
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }


    public List<AssignmentDTO> getAll(Long tenantId, boolean activeOnly) {
        return dao.getAll(tenantId, activeOnly).stream()
                .map(mapper::toDto)
                .toList();
    }


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
        assignment.setState(dto.getState() == null ? AssignmentState.PLANNED : dto.getState());
        applyDetails(assignment, dto, tenantId);
        Instant now = Instant.now(clock);
        AssignmentStateHistory history = new AssignmentStateHistory(
                null, null, assignment.getState(), "system", now);
        try {
            return mapper.toDto(dao.create(assignment, history));
        } catch (PersistenceException e) {

            rejectDuplicate(tenantId, name, null);
            throw e;
        }
    }

    
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


    public AssignmentDTO deactivate(Long id, Long tenantId) {
        return setActive(id, tenantId, false);
    }

    public AssignmentDTO activate(Long id, Long tenantId) {
        return setActive(id, tenantId, true);
    }

    public AssignmentDTO changeState(Long id, AssignmentState nextState, Long tenantId, String source) {
        if (nextState == null) {
            throw new ApiException(400, "Assignment state is required");
        }
        Assignment existing = find(id, tenantId);
        return changeState(existing, nextState, source);
    }

    public AssignmentDTO checkIn(Long id, Long tenantId, Long callerId, boolean admin, String source) {
        Assignment existing = find(id, tenantId);
        if (!admin) {
            if (!existing.isActive() || callerId == null || !callerId.equals(existing.getAssignedEmployeeId())) {
                throw notFound();
            }
        }
        if (existing.getCheckInAt() != null && existing.getCheckOutAt() == null) {
            throw new ApiException(409, "Assignment is already checked in");
        }
        if (!isCheckInEligible(existing.getState())) {
            throw new ApiException(409, "Assignment is not eligible for check-in");
        }
        if (existing.getCheckInAt() != null) {
            throw new ApiException(409, "Assignment is already checked in");
        }
        Instant now = Instant.now(clock);
        existing.setCheckInAt(now);
        if (existing.getState() != AssignmentState.IN_PROGRESS) {
            return changeState(existing, AssignmentState.IN_PROGRESS, source, now);
        }
        return mapper.toDto(dao.update(existing));
    }

    public AssignmentDTO checkOut(Long id, Long tenantId, String source) {
        Assignment existing = find(id, tenantId);
        if (existing.getCheckInAt() == null) {
            throw new ApiException(409, "Assignment must be checked in before checkout");
        }
        if (existing.getCheckOutAt() != null) {
            throw new ApiException(409, "Assignment is already checked out");
        }
        Instant now = Instant.now(clock);
        existing.setCheckOutAt(now);
        if (existing.getState() != AssignmentState.COMPLETED) {
            return changeState(existing, AssignmentState.COMPLETED, source, now);
        }
        return mapper.toDto(dao.update(existing));
    }

    public List<AssignmentStateHistoryDTO> getStateHistory(Long id, Long tenantId) {
        Assignment assignment = find(id, tenantId);
        return dao.getStateHistory(assignment.getId()).stream()
                .map(mapper::toDto)
                .toList();
    }


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

    private AssignmentDTO changeState(Assignment assignment, AssignmentState nextState, String source) {
        return changeState(assignment, nextState, source, Instant.now(clock));
    }

    private AssignmentDTO changeState(Assignment assignment, AssignmentState nextState, String source, Instant changedAt) {
        AssignmentState previous = assignment.getState() == null ? AssignmentState.PLANNED : assignment.getState();
        if (previous == nextState) {
            return mapper.toDto(assignment);
        }
        if (!previous.canTransitionTo(nextState)) {
            throw new ApiException(409, "Assignment state cannot transition from " + previous + " to " + nextState);
        }
        assignment.setState(nextState);
        AssignmentStateHistory history = new AssignmentStateHistory(
                assignment.getId(), previous, nextState, validSource(source), changedAt);
        return mapper.toDto(dao.update(assignment, history));
    }

    private static boolean isCheckInEligible(AssignmentState state) {
        AssignmentState current = state == null ? AssignmentState.PLANNED : state;
        return current == AssignmentState.PLANNED
                || current == AssignmentState.ACKNOWLEDGED
                || current == AssignmentState.AUTO_ACCEPTED;
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

    private static String validSource(String source) {
        String trimmed = source == null ? "" : source.trim();
        return trimmed.isEmpty() ? "system" : trimmed;
    }


    private void applyDetails(Assignment assignment, AssignmentDTO dto, Long tenantId) {
        String address = validAddress(dto.getAddress());
        Integer estimatedMinutes = validEstimatedMinutes(dto.getEstimatedMinutes());
        BigDecimal cost = validCost(dto.getCost());
        var startTime = dto.getStartTime();
        var estimatedEndTime = validEstimatedEndTime(startTime, dto.getEstimatedEndTime());
        Long employeeId = validEmployee(dto.getAssignedEmployeeId(), assignment.getAssignedEmployeeId(), tenantId);
        List<Long> productIds = validProductIds(dto.getProductIds(), tenantId);

        assignment.setAddress(address);
        assignment.setEstimatedMinutes(estimatedMinutes);
        assignment.setCost(cost);
        assignment.setStartTime(startTime);
        assignment.setEstimatedEndTime(estimatedEndTime);
        assignment.setAssignedEmployeeId(employeeId);
        assignment.setProductIds(productIds);
    }

    private static java.time.LocalDateTime validEstimatedEndTime(
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime estimatedEndTime) {
        if (startTime != null && estimatedEndTime != null && !estimatedEndTime.isAfter(startTime)) {
            throw new ApiException(400, "Estimated end time must be later than start time");
        }
        return estimatedEndTime;
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

    private List<Long> validProductIds(List<Long> productIds, Long tenantId) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(productIds);
        if (uniqueIds.contains(null)) {
            throw new ApiException(400, "Product id is required");
        }
        for (Long productId : uniqueIds) {
            Product product = productDAO.findById(productId);
            if (product == null || !tenantId.equals(product.getTenantId())) {
                throw new ApiException(400, "Product not found, it may not belong to you");
            }
        }
        return List.copyOf(uniqueIds);
    }


    private void rejectDuplicate(Long tenantId, String name, Long ownId) {
        boolean taken = dao.findByName(tenantId, name).stream()
                .anyMatch(other -> !other.getId().equals(ownId));
        if (taken) {
            throw new ApiException(409, "An assignment with this name already exists");
        }
    }
}

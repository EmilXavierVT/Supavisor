package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.ProductDAO;
import app.dao.UserDAO;
import app.dto.AssignmentAuditHistoryDTO;
import app.dto.AssignmentDTO;
import app.dto.AssignmentOverlapDTO;
import app.dto.AssignmentResourceRequirementDTO;
import app.dto.AssignmentStateHistoryDTO;
import app.dto.AttendanceCorrectionDTO;
import app.entities.Assignment;
import app.entities.AssignmentAuditHistory;
import app.entities.AssignmentResourceMode;
import app.entities.AssignmentResourceRequirement;
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
import java.util.Objects;
import java.util.stream.Collectors;


public class AssignmentService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_NOTES_LENGTH = 1000;
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

    public List<AssignmentDTO> getVisibleToUser(Long tenantId, Long callerId, boolean activeOnly) {
        User caller = userDAO.getById(callerId);
        if (caller == null || !Objects.equals(caller.getTenantId(), tenantId)) {
            throw notFound();
        }
        String category = caller.getPrimaryCategory();
        return dao.getVisibleForCategory(tenantId, category, activeOnly).stream()
                .map(this::toCategoryScheduleDto)
                .toList();
    }


    public AssignmentDTO getById(Long id, Long tenantId, boolean activeOnly) {
        Assignment assignment = find(id, tenantId);
        if (activeOnly && !assignment.isActive()) {
            throw notFound();
        }
        return mapper.toDto(assignment);
    }

    public AssignmentDTO getVisibleById(Long id, Long tenantId, Long callerId) {
        Assignment assignment = find(id, tenantId);
        if (!assignment.isActive()) {
            throw notFound();
        }
        User caller = userDAO.getById(callerId);
        User assigned = assignment.getAssignedEmployeeId() == null ? null : userDAO.getById(assignment.getAssignedEmployeeId());
        if (caller == null || assigned == null
                || !Objects.equals(caller.getTenantId(), tenantId)
                || !Objects.equals(assigned.getTenantId(), tenantId)) {
            throw notFound();
        }
        if (Objects.equals(callerId, assignment.getAssignedEmployeeId())) {
            AssignmentDTO dto = mapper.toDto(assignment);
            dto.setCost(null);
            return dto;
        }
        if (!sameCategory(caller.getPrimaryCategory(), assigned.getPrimaryCategory())) {
            throw notFound();
        }
        return toCategoryScheduleDto(assignment);
    }

    public AssignmentDTO create(AssignmentDTO dto, Long tenantId) {
        return create(dto, tenantId, null, "system");
    }

    public AssignmentDTO create(AssignmentDTO dto, Long tenantId, Long actorUserId, String actorSource) {
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, null);

        Assignment assignment = new Assignment();
        assignment.setName(name);
        assignment.setTenantId(tenantId);
        assignment.setActive(dto.getIsActive() == null || dto.getIsActive());
        assignment.setState(dto.getState() == null ? AssignmentState.PLANNED : dto.getState());
        applyDetails(assignment, dto, tenantId);
        List<Assignment> conflicts = overlappingAssignments(assignment, null);
        AssignmentAuditHistory overlapAudit = overlapAudit(dto.getOverrideReason(), conflicts, actorUserId, actorSource);
        Instant now = Instant.now(clock);
        AssignmentStateHistory history = new AssignmentStateHistory(
                null, null, assignment.getState(), "system", now);
        try {
            Assignment created = dao.create(assignment, history);
            if (overlapAudit != null) {
                overlapAudit.setAssignmentId(created.getId());
                dao.update(created, overlapAudit);
            }
            return mapper.toDto(created);
        } catch (PersistenceException e) {

            rejectDuplicate(tenantId, name, null);
            throw e;
        }
    }

    
    public AssignmentDTO update(Long id, AssignmentDTO dto, Long tenantId) {
        return update(id, dto, tenantId, null, "system");
    }

    public AssignmentDTO update(Long id, AssignmentDTO dto, Long tenantId, Long actorUserId, String actorSource) {
        Assignment existing = find(id, tenantId);
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, id);

        existing.setName(name);
        applyDetails(existing, dto, tenantId);
        if (dto.getIsActive() != null) {
            existing.setActive(dto.getIsActive());
        }
        List<Assignment> conflicts = overlappingAssignments(existing, id);
        AssignmentAuditHistory overlapAudit = overlapAudit(dto.getOverrideReason(), conflicts, actorUserId, actorSource);
        try {
            return mapper.toDto(dao.update(existing, overlapAudit));
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
        Long employeeId = attendanceEmployeeId(existing, callerId, admin);
        Assignment active = dao.findActiveCheckInForEmployee(tenantId, employeeId, existing.getId());
        if (active != null) {
            throw new ApiException(409, "Employee already has an active attendance record for assignment " + active.getId());
        }
        if (existing.getCheckInAt() != null) {
            throw new ApiException(409, "Assignment is already checked in");
        }
        Instant now = Instant.now(clock);
        if (existing.getAssignedEmployeeId() == null) {
            existing.setAssignedEmployeeId(employeeId);
        }
        existing.setCheckInAt(now);
        if (existing.getState() != AssignmentState.IN_PROGRESS) {
            return changeState(existing, AssignmentState.IN_PROGRESS, source, now);
        }
        return mapper.toDto(dao.update(existing));
    }

    public AssignmentDTO checkOut(Long id, Long tenantId, Long callerId, boolean admin, String source) {
        Assignment existing = find(id, tenantId);
        attendanceEmployeeId(existing, callerId, admin);
        if (existing.getCheckInAt() == null) {
            throw new ApiException(409, "Assignment has no active check-in");
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

    public List<AssignmentAuditHistoryDTO> getAttendanceHistory(Long id, Long tenantId, Long callerId, boolean admin) {
        Assignment assignment = find(id, tenantId);
        if (!admin && (callerId == null || !callerId.equals(assignment.getAssignedEmployeeId()))) {
            throw notFound();
        }
        return dao.getAuditHistory(assignment.getId(), "ATTENDANCE_CORRECTION").stream()
                .map(mapper::toDto)
                .toList();
    }

    public List<AssignmentOverlapDTO> previewOverlaps(AssignmentDTO dto, Long tenantId, Long ownId) {
        Assignment probe = new Assignment();
        probe.setTenantId(tenantId);
        applyDetails(probe, dto, tenantId);
        return overlappingAssignments(probe, ownId).stream()
                .map(this::toOverlapDto)
                .toList();
    }

    public AssignmentDTO correctAttendance(Long id, Long tenantId, Long actorUserId, String actorSource,
                                           AttendanceCorrectionDTO dto) {
        Assignment existing = find(id, tenantId);
        if (dto == null) {
            throw new ApiException(400, "Attendance correction is required");
        }
        String reason = validCorrectionReason(dto.getReason());
        Instant nextCheckIn = dto.getCheckInAt() == null ? existing.getCheckInAt() : dto.getCheckInAt();
        Instant nextCheckOut = dto.getCheckOutAt() == null ? existing.getCheckOutAt() : dto.getCheckOutAt();
        if (dto.getCheckInAt() == null && dto.getCheckOutAt() == null) {
            throw new ApiException(400, "Corrected check-in or check-out time is required");
        }
        if (nextCheckIn != null && nextCheckOut != null && !nextCheckOut.isAfter(nextCheckIn)) {
            throw new ApiException(400, "Check-out must be after check-in");
        }
        String details = "checkInAt: " + existing.getCheckInAt() + " -> " + nextCheckIn
                + "; checkOutAt: " + existing.getCheckOutAt() + " -> " + nextCheckOut;
        existing.setCheckInAt(nextCheckIn);
        existing.setCheckOutAt(nextCheckOut);
        AssignmentAuditHistory audit = audit("ATTENDANCE_CORRECTION", actorUserId, actorSource, reason, details);
        return mapper.toDto(dao.update(existing, audit));
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

    private Assignment find(Long id, Long tenantId) {
        Assignment assignment = id == null ? null : dao.getById(id);
        if (assignment == null || !assignment.getTenantId().equals(tenantId)) {
            throw notFound();
        }
        return assignment;
    }

    private Long attendanceEmployeeId(Assignment assignment, Long callerId, boolean admin) {
        Long employeeId = assignment.getAssignedEmployeeId();
        if (employeeId == null) {
            employeeId = callerId;
        }
        if (employeeId == null) {
            throw new ApiException(400, "Employee is required for attendance");
        }
        if (!admin && assignment.getAssignedEmployeeId() != null && !employeeId.equals(callerId)) {
            throw notFound();
        }
        if (!admin && !assignment.isActive()) {
            throw notFound();
        }
        return employeeId;
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
        String notes = validNotes(dto.getNotes());
        Integer estimatedMinutes = validEstimatedMinutes(dto.getEstimatedMinutes());
        BigDecimal cost = validCost(dto.getCost());
        var startTime = dto.getStartTime();
        var estimatedEndTime = validEstimatedEndTime(startTime, dto.getEstimatedEndTime());
        Long employeeId = validEmployee(dto.getAssignedEmployeeId(), assignment.getAssignedEmployeeId(), tenantId);
        List<Long> productIds = validProductIds(dto.getProductIds(), tenantId);
        List<AssignmentResourceRequirement> resources = validResourceRequirements(dto.getResourceRequirements(), tenantId);

        assignment.setAddress(address);
        assignment.setNotes(notes);
        assignment.setEstimatedMinutes(estimatedMinutes);
        assignment.setCost(cost);
        assignment.setStartTime(startTime);
        assignment.setEstimatedEndTime(estimatedEndTime);
        assignment.setAssignedEmployeeId(employeeId);
        assignment.setProductIds(productIds);
        assignment.setResourceRequirements(resources);
    }

    private List<Assignment> overlappingAssignments(Assignment assignment, Long ownId) {
        return dao.findOverlappingAssignments(
                assignment.getTenantId(),
                assignment.getAssignedEmployeeId(),
                assignment.getStartTime(),
                assignment.getEstimatedEndTime(),
                ownId);
    }

    private AssignmentAuditHistory overlapAudit(String overrideReason, List<Assignment> conflicts,
                                                Long actorUserId, String actorSource) {
        if (conflicts.isEmpty()) {
            return null;
        }
        String details = conflicts.stream()
                .map(conflict -> conflict.getId() + " " + conflict.getName()
                        + " " + conflict.getStartTime() + "-" + conflict.getEstimatedEndTime())
                .collect(Collectors.joining("; "));
        String reason = overlapReason(overrideReason, details);
        return audit("OVERLAP_OVERRIDE", actorUserId, actorSource, reason, details);
    }

    private AssignmentAuditHistory audit(String type, Long actorUserId, String actorSource, String reason, String details) {
        return AssignmentAuditHistory.builder()
                .auditType(type)
                .actorUserId(actorUserId)
                .actorSource(validSource(actorSource))
                .reason(reason)
                .details(details)
                .createdAt(Instant.now(clock))
                .build();
    }

    private AssignmentDTO toCategoryScheduleDto(Assignment assignment) {
        AssignmentDTO dto = mapper.toDto(assignment);
        dto.setCost(null);
        dto.setProductIds(List.of());
        dto.setResourceRequirements(List.of());
        return dto;
    }

    private AssignmentOverlapDTO toOverlapDto(Assignment assignment) {
        return new AssignmentOverlapDTO(
                assignment.getId(),
                assignment.getName(),
                assignment.getAssignedEmployeeId(),
                assignment.getStartTime(),
                assignment.getEstimatedEndTime());
    }

    private static boolean sameCategory(String first, String second) {
        if (first == null || first.isBlank() || second == null || second.isBlank()) {
            return false;
        }
        return first.trim().equalsIgnoreCase(second.trim());
    }

    private static java.time.LocalDateTime validEstimatedEndTime(
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime estimatedEndTime) {
        if (startTime != null && estimatedEndTime != null && !estimatedEndTime.isAfter(startTime)) {
            throw new ApiException(400, "Estimated end time must be later than start time");
        }
        return estimatedEndTime;
    }

    private static String validReason(String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(409, "Overlap override or attendance correction reason is required");
        }
        if (trimmed.length() > 1000) {
            throw new ApiException(400, "Reason must be at most 1000 characters");
        }
        return trimmed;
    }

    private static String overlapReason(String reason, String details) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(409, "Assignment overlaps with: " + details);
        }
        return validReason(reason);
    }

    private static String validCorrectionReason(String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(400, "Attendance correction reason is required");
        }
        if (trimmed.length() > 1000) {
            throw new ApiException(400, "Reason must be at most 1000 characters");
        }
        return trimmed;
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

    private static String validNotes(String notes) {
        String trimmed = notes == null ? "" : notes.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new ApiException(400, "Notes must be at most " + MAX_NOTES_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Notes contains invalid characters");
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

    private List<AssignmentResourceRequirement> validResourceRequirements(
            List<AssignmentResourceRequirementDTO> resourceRequirements,
            Long tenantId) {
        if (resourceRequirements == null || resourceRequirements.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        java.util.ArrayList<AssignmentResourceRequirement> valid = new java.util.ArrayList<>();
        for (AssignmentResourceRequirementDTO resource : resourceRequirements) {
            Long productId = resource == null ? null : resource.getProductId();
            AssignmentResourceMode mode = resource == null ? null : resource.getMode();
            if (productId == null) {
                throw new ApiException(400, "Resource product id is required");
            }
            if (mode == null) {
                throw new ApiException(400, "Resource mode is required");
            }
            if (!uniqueIds.add(productId)) {
                throw new ApiException(400, "Resource product id cannot be repeated");
            }
            Product product = productDAO.findById(productId);
            if (product == null || !tenantId.equals(product.getTenantId())) {
                throw new ApiException(400, "Resource not found, it may not belong to you");
            }
            valid.add(new AssignmentResourceRequirement(productId, mode));
        }
        return List.copyOf(valid);
    }


    private void rejectDuplicate(Long tenantId, String name, Long ownId) {
        boolean taken = dao.findByName(tenantId, name).stream()
                .anyMatch(other -> !other.getId().equals(ownId));
        if (taken) {
            throw new ApiException(409, "An assignment with this name already exists");
        }
    }
}

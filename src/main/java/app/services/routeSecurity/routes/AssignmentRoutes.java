package app.services.routeSecurity.routes;

import app.dto.AssignmentDTO;
import app.dto.AssignmentHistoryDTO;
import app.dto.UserDTO;
import app.entities.Assignment;
import app.entities.AssignmentHistory;
import app.exceptions.ApiException;
import app.services.entityServices.AssignmentService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class AssignmentRoutes {

    private final AssignmentService assignmentService;

    public AssignmentRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.assignmentService = new AssignmentService(emf);
    }

    /** Administrators see everything; everyone else only the active ones. */
    public void getAll(Context ctx) {
        boolean activeOnly = !isAdmin(ctx) || Boolean.parseBoolean(ctx.queryParam("activeOnly"));
        List<Assignment> entities = assignmentService.getAll(callerTenantId(ctx), activeOnly);

        List<AssignmentDTO> dtos = entities.stream()
                .map(AssignmentDTO::new)
                .toList();

        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Assignment entity = assignmentService.getById(id, callerTenantId(ctx));

        ctx.json(new AssignmentDTO(entity));
    }

    public void create(Context ctx) {
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        Assignment assignment = mapToEntity(dto, callerTenantId(ctx));

        Assignment createdEntity = assignmentService.create(assignment, callerUsername(ctx));
        ctx.status(201).json(new AssignmentDTO(createdEntity));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        Assignment assignment = mapToEntity(dto, callerTenantId(ctx));
        assignment.setId(id);

        Assignment updatedEntity = assignmentService.update(assignment, callerUsername(ctx));
        ctx.json(new AssignmentDTO(updatedEntity));
    }

    public void deactivate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Assignment cancelledEntity = assignmentService.cancel(id, callerTenantId(ctx), callerUsername(ctx));
        ctx.json(new AssignmentDTO(cancelledEntity));
    }

    public void activate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Assignment existing = assignmentService.getById(id, callerTenantId(ctx));
        existing.setActive(true);
        Assignment updatedEntity = assignmentService.update(existing, callerUsername(ctx));
        ctx.json(new AssignmentDTO(updatedEntity));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        assignmentService.delete(id, callerTenantId(ctx), callerUsername(ctx));
        ctx.status(204);
    }

    public void reassign(Context ctx) {
        Long assignmentId = ctx.pathParamAsClass("id", Long.class).get();
        Long employeeId = ctx.queryParamAsClass("employeeId", Long.class).get();

        Assignment reassignedEntity = assignmentService.reassign(assignmentId, employeeId, callerTenantId(ctx), callerUsername(ctx));
        ctx.json(new AssignmentDTO(reassignedEntity));
    }

    public void removeEmployee(Context ctx) {
        Long assignmentId = ctx.pathParamAsClass("id", Long.class).get();
        Assignment updatedEntity = assignmentService.removeEmployee(assignmentId, callerTenantId(ctx), callerUsername(ctx));
        ctx.json(new AssignmentDTO(updatedEntity));
    }

    public void getHistory(Context ctx) {
        Long assignmentId = ctx.pathParamAsClass("id", Long.class).get();

        List<AssignmentHistory> historyEntities = assignmentService.getHistory(assignmentId, callerTenantId(ctx));

        List<AssignmentHistoryDTO> dtos = historyEntities.stream()
                .map(AssignmentHistoryDTO::new)
                .toList();

        ctx.json(dtos);
    }

    // --- Helper Methods ---

    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }

    private static String callerUsername(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null) {
            return "SYSTEM";
        }
        if (caller.getName() != null && !caller.getName().isBlank()) {
            return caller.getName();
        }
        if (caller.getEmail() != null && !caller.getEmail().isBlank()) {
            return caller.getEmail();
        }
        return caller.getId() != null ? String.valueOf(caller.getId()) : "SYSTEM";
    }

    private static boolean isAdmin(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        return caller != null && caller.getRoles() != null
                && caller.getRoles().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }

    private static Assignment mapToEntity(AssignmentDTO dto, Long tenantId) {
        Assignment assignment = new Assignment();
        if (dto.getId() != null) {
            assignment.setId(dto.getId());
        }
        assignment.setName(dto.getName());
        assignment.setTenantId(tenantId);
        assignment.setAssignedEmployeeId(dto.getAssignedEmployeeId());

        if (dto.getIsActive() != null) {
            assignment.setActive(dto.getIsActive());
        }
        if (dto.getIsFlagged() != null) {
            assignment.setFlagged(dto.getIsFlagged());
        }
        assignment.setAddress(dto.getAddress());
        assignment.setEstimatedMinutes(dto.getEstimatedMinutes());
        assignment.setCost(dto.getCost());

        if (dto.getMissingEmployeeCount() != null) {
            assignment.setMissingEmployeeCount(dto.getMissingEmployeeCount());
        }

        return assignment;
    }
}
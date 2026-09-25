package app.services.routeSecurity.routes;

import app.dto.AssignmentDTO;
import app.dto.UserDTO;
import app.entities.Assignment;
import app.exceptions.ApiException;
import app.services.entityServices.AssignmentService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class AssignmentRoutes {

    private final AssignmentService assignmentService;

    public AssignmentRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.assignmentService = new AssignmentService(emf);
    }

    /** Administrators see everything (or only the active ones with ?activeOnly=true); everyone else only the active ones. */
    public void getAll(Context ctx) {
        boolean activeOnly = !isAdmin(ctx) || Boolean.parseBoolean(ctx.queryParam("activeOnly"));
        ctx.json(assignmentService.getAll(callerTenantId(ctx), activeOnly));
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        // Aligned: getById(id, tenantId) [2 parameters]
        ctx.json(assignmentService.getById(id, callerTenantId(ctx)));
    }

    public void create(Context ctx) {
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        Assignment assignment = mapToEntity(dto, callerTenantId(ctx));

        // Aligned: passes (Assignment, String changedBy)
        ctx.status(201).json(assignmentService.create(assignment, callerUsername(ctx)));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        Assignment assignment = mapToEntity(dto, callerTenantId(ctx));
        assignment.setId(id);

        // Aligned: passes (Assignment, String changedBy)
        ctx.json(assignmentService.update(assignment, callerUsername(ctx)));
    }

    public void deactivate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        // Maps deactivate to service cancel workflow
        ctx.json(assignmentService.cancel(id, callerTenantId(ctx), callerUsername(ctx)));
    }

    public void activate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Assignment existing = assignmentService.getById(id, callerTenantId(ctx));
        existing.setActive(true);
        ctx.json(assignmentService.update(existing, callerUsername(ctx)));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        // Aligned: passes (id, tenantId, String changedBy)
        assignmentService.delete(id, callerTenantId(ctx), callerUsername(ctx));
        ctx.status(204);
    }

    // PUT /assignments/{id}/reassign?employeeId=42
    public void reassign(Context ctx) {
        Long assignmentId = ctx.pathParamAsClass("id", Long.class).get();
        Long employeeId = ctx.queryParamAsClass("employeeId", Long.class).get();

        // Aligned: passes 4 parameters (assignmentId, newEmployeeId, tenantId, String changedBy)
        ctx.json(assignmentService.reassign(assignmentId, employeeId, callerTenantId(ctx), callerUsername(ctx)));
    }

    // DELETE /assignments/{id}/employee
    public void removeEmployee(Context ctx) {
        Long assignmentId = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.removeEmployee(assignmentId, callerTenantId(ctx), callerUsername(ctx)));
    }

    // GET /assignments/{id}/history
    public void getHistory(Context ctx) {
        Long assignmentId = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.getHistory(assignmentId, callerTenantId(ctx)));
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
        assignment.setTenantId(tenantId);
        assignment.setAssignedEmployeeId(dto.getAssignedEmployeeId());

        // Updated getter names
        assignment.setActive(dto.getIsActive());
        assignment.setFlagged(dto.getIsFlagged());

        return assignment;
    }
}
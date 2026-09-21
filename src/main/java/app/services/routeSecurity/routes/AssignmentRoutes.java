package app.services.routeSecurity.routes;

import app.dto.AssignmentDTO;
import app.dto.UserDTO;
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
        ctx.json(assignmentService.getById(id, callerTenantId(ctx), !isAdmin(ctx)));
    }

    public void create(Context ctx) {
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        ctx.status(201).json(assignmentService.create(dto, callerTenantId(ctx)));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        ctx.json(assignmentService.update(id, dto, callerTenantId(ctx)));
    }

    public void deactivate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.deactivate(id, callerTenantId(ctx)));
    }

    public void activate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.activate(id, callerTenantId(ctx)));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        assignmentService.delete(id, callerTenantId(ctx));
        ctx.status(204);
    }

    // the tenant comes from the token, never from the request
    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }

    private static boolean isAdmin(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        return caller != null && caller.getRoles() != null
                && caller.getRoles().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }
}

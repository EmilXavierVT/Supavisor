package app.services.routeSecurity.routes;

import app.dto.ProjectDTO;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.entityServices.ProjectService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class ProjectRoutes {

    private final ProjectService projectService;

    public ProjectRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.projectService = new ProjectService(emf);
    }

    public void getAll(Context ctx) {
        ctx.json(projectService.getAll(callerTenantId(ctx)));
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(projectService.getById(id, callerTenantId(ctx)));
    }

    public void create(Context ctx) {
        ProjectDTO dto = ctx.bodyValidator(ProjectDTO.class).get();
        ctx.status(201).json(projectService.create(dto, callerTenantId(ctx)));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ProjectDTO dto = ctx.bodyValidator(ProjectDTO.class).get();
        ctx.json(projectService.update(id, dto, callerTenantId(ctx)));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        projectService.delete(id, callerTenantId(ctx));
        ctx.status(204);
    }

    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }
}

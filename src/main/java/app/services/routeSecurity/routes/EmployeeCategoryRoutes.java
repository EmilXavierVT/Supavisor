package app.services.routeSecurity.routes;

import app.dto.EmployeeCategoryDTO;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.entityServices.EmployeeCategoryService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class EmployeeCategoryRoutes {
    private final EmployeeCategoryService service;

    public EmployeeCategoryRoutes(EntityManagerFactory emf) {
        this.service = new EmployeeCategoryService(emf);
    }

    public void getAll(Context ctx) {
        ctx.json(service.getAll(callerTenantId(ctx), Boolean.parseBoolean(ctx.queryParam("activeOnly"))));
    }

    public void create(Context ctx) {
        EmployeeCategoryDTO dto = ctx.bodyValidator(EmployeeCategoryDTO.class).get();
        ctx.status(201).json(service.create(dto, callerTenantId(ctx)));
    }

    public void deactivate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(service.setActive(id, callerTenantId(ctx), false));
    }

    public void activate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(service.setActive(id, callerTenantId(ctx), true));
    }

    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }
}

package app.services.routeSecurity.routes;
import app.dto.RoleDTO;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.entityServices.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;

import jakarta.persistence.EntityManagerFactory;

public class RoleRoutes {

    private final RoleService roleService;

    ObjectMapper objectMapper = new ObjectMapper();

    public RoleRoutes(EntityManagerFactory emf) {
        this.roleService = new RoleService(emf);
    }


    protected void getRolesByTenant(Context ctx) {
        Long tenantId = Long.parseLong(ctx.pathParam("tenantId"));
        if (!tenantId.equals(callerTenantId(ctx))) {
            throw new ApiException(403, "You can only see your own company's roles");
        }
        ctx.json(roleService.getRolesByTenant(tenantId));
    }

    protected void createRole(Context ctx) {
        Long tenantId = callerTenantId(ctx);

        RoleDTO roleDTO = ctx.bodyAsClass(RoleDTO.class);
        ctx.json(roleService.createRole(roleDTO, tenantId));
    }

    protected void deleteRole(Context ctx) {
        Long roleId = Long.parseLong(ctx.pathParam("id"));
        // the tenant comes from the token, never from the request, so one company cannot delete another's roles
        roleService.deleteRole(roleId, callerTenantId(ctx));
        ctx.status(204);
    }

    private Long callerTenantId(Context ctx) {
        UserDTO user = ctx.attribute("user");
        if (user == null || user.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return user.getTenantId();
    }
}

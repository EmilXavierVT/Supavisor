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
        ctx.json(roleService.getRolesByTenant(tenantId));
    }

    protected void createRole(Context ctx) {
        UserDTO user = ctx.attribute("user");
        if (user == null || user.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        Long tenantId = user.getTenantId();

        RoleDTO roleDTO = ctx.bodyAsClass(RoleDTO.class);
        ctx.json(roleService.createRole(roleDTO, tenantId));
    }

    protected void deleteRole(Context ctx) {
        Long roleId = Long.parseLong(ctx.pathParam("id"));
        Long tenantId = Long.parseLong(ctx.queryParam("tenantId"));
        roleService.deleteRole(roleId, tenantId);
        ctx.status(204);
    }
}

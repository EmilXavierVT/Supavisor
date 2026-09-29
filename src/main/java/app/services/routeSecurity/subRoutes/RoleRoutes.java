package app.services.routeSecurity.subRoutes;

import app.controller.RoleController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.delete;

public class RoleRoutes {

    RoleController roleController;
    public RoleRoutes(EntityManagerFactory emf) {
        this.roleController = new RoleController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("/role", () -> {
            get("/tenant/{tenantId}", roleController::getRolesByTenant, Role.USER, Role.ADMIN);
            post("/", roleController::createRole, Role.ADMIN);
            delete("/{id}", roleController::deleteRole, Role.ADMIN);
        });
    }
}

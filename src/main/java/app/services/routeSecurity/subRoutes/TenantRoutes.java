package app.services.routeSecurity.subRoutes;

import app.controller.TenantController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;

import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class TenantRoutes {

    private final TenantController tenantController;

    public TenantRoutes(EntityManagerFactory emf) {
        this.tenantController = new TenantController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("/tenant", () -> {
            get("/all", tenantController::getAll, Role.ADMIN, Role.USER);
            post("/", tenantController::create, Role.ADMIN);
            get("/{id}", tenantController::getById, Role.USER, Role.ADMIN);
            put("/{id}", tenantController::update, Role.ADMIN);
            delete("/{id}", tenantController::delete, Role.ADMIN);
        });
    }
}
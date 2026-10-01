package app.services.routeSecurity.subRoutes;

import app.controller.ProjectController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.put;

public class ProjectRoutes {

    ProjectController projectController;

    public ProjectRoutes(EntityManagerFactory emf) {
        this.projectController = new ProjectController(emf);

    }

    public EndpointGroup getRoutes() {
        return () -> path("/project",() -> {
            get("/all", projectController::getAll, Role.ADMIN, Role.USER);
            get("/{id}/status-history", projectController::getStatusHistory, Role.ADMIN, Role.USER);
            get("/{id}", projectController::getById, Role.ADMIN, Role.USER);
            post("/", projectController::create, Role.ADMIN);
            put("/{id}", projectController::update, Role.ADMIN);
            delete("/{id}", projectController::delete, Role.ADMIN);
        });
    }
}

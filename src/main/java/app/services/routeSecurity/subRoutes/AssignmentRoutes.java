package app.services.routeSecurity.subRoutes;

import app.controller.AssignmentController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.put;

public class AssignmentRoutes {

    private final AssignmentController assignmentController;

    public AssignmentRoutes(EntityManagerFactory emf) {
        this.assignmentController = new AssignmentController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("assignment", () -> {
            get("/all", assignmentController::getAll, Role.ADMIN, Role.USER);
            get("/{id}", assignmentController::getById, Role.ADMIN, Role.USER);
            post("/", assignmentController::create, Role.ADMIN);
            put("/{id}", assignmentController::update, Role.ADMIN);
            patch("/{id}/deactivate", assignmentController::deactivate, Role.ADMIN);
            patch("/{id}/activate", assignmentController::activate, Role.ADMIN);
            delete("/{id}", assignmentController::delete, Role.ADMIN);
        });

    }
}

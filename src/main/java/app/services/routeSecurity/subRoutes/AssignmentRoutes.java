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
            post("/overlaps", assignmentController::previewOverlaps, Role.ADMIN);
            get("/{id}", assignmentController::getById, Role.ADMIN, Role.USER);
            post("/", assignmentController::create, Role.ADMIN);
            put("/{id}", assignmentController::update, Role.ADMIN);
            put("/{id}/responsible", assignmentController::setResponsible, Role.ADMIN);
            delete("/{id}/responsible", assignmentController::clearResponsible, Role.ADMIN);
            get("/{id}/delegations", assignmentController::getDelegations, Role.ADMIN);
            post("/{id}/delegations", assignmentController::delegate, Role.ADMIN);
            delete("/{id}/delegations/{employeeId}", assignmentController::undelegate, Role.ADMIN);
            patch("/{id}/deactivate", assignmentController::deactivate, Role.ADMIN);
            patch("/{id}/activate", assignmentController::activate, Role.ADMIN);
            patch("/{id}/state", assignmentController::changeState, Role.ADMIN);
            patch("/{id}/check-in", assignmentController::checkIn, Role.ADMIN, Role.USER);
            patch("/{id}/check-out", assignmentController::checkOut, Role.ADMIN, Role.USER);
            patch("/{id}/attendance-correction", assignmentController::correctAttendance, Role.ADMIN);
            get("/{id}/attendance-history", assignmentController::getAttendanceHistory, Role.ADMIN, Role.USER);
            get("/{id}/history", assignmentController::getHistory, Role.ADMIN);
            get("/{id}/state-history", assignmentController::getStateHistory, Role.ADMIN, Role.USER);
            delete("/{id}", assignmentController::delete, Role.ADMIN);
        });

    }
}

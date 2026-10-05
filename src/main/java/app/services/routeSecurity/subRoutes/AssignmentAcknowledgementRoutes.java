package app.services.routeSecurity.subRoutes;

import app.controller.AssignmentAcknowledgementController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class AssignmentAcknowledgementRoutes {

    private final AssignmentAcknowledgementController acknowledgementController;

    public AssignmentAcknowledgementRoutes(EntityManagerFactory entityManagerFactory) {
        acknowledgementController = new AssignmentAcknowledgementController(entityManagerFactory);
    }

    public EndpointGroup getRoutes() {
        return () -> path("assignment-acknowledgements", () ->
                post("/", acknowledgementController::create, Role.USER, Role.EMPLOYEE)
        );
    }
}

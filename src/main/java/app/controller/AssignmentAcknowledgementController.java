package app.controller;

import app.dto.AssignmentAcknowledgementRequestDTO;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.entityServices.AssignmentAcknowledgementService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class AssignmentAcknowledgementController {

    private final AssignmentAcknowledgementService acknowledgementService;

    public AssignmentAcknowledgementController(EntityManagerFactory entityManagerFactory) {
        acknowledgementService = new AssignmentAcknowledgementService(entityManagerFactory);
    }

    public void create(Context context) {
        UserDTO caller = context.attribute("user");
        if (caller == null || caller.getId() == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated");
        }

        AssignmentAcknowledgementRequestDTO request = context.bodyValidator(
                AssignmentAcknowledgementRequestDTO.class
        ).get();
        context.status(201).json(acknowledgementService.acknowledge(
                request.getAssignmentId(),
                caller.getTenantId(),
                caller.getId()
        ));
    }
}

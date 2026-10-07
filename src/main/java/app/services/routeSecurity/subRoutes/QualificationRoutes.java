package app.services.routeSecurity.subRoutes;

import app.controller.QualificationController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class QualificationRoutes {
    private final QualificationController qualificationController;

    public QualificationRoutes(EntityManagerFactory emf) {
        this.qualificationController = new QualificationController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("/qualification", () -> {
            get("/all", qualificationController::getAll, Role.USER, Role.ADMIN);
            post("/", qualificationController::create, Role.ADMIN);
            put("/{id}", qualificationController::update, Role.ADMIN);
            delete("/{id}", qualificationController::delete, Role.ADMIN);
        });
    }
}

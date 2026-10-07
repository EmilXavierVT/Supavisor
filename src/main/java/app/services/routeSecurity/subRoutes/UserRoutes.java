package app.services.routeSecurity.subRoutes;

import app.controller.UserController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;

import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class UserRoutes {

    private final UserController userController;
    private final app.controller.QualificationController qualificationController;

    public UserRoutes(EntityManagerFactory emf) {
        this.userController = new UserController(emf);
        this.qualificationController = new app.controller.QualificationController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("/user", () -> {
            post("/", userController::createUser, Role.ADMIN);
            put("/update", userController::update, Role.ADMIN);
            post("/create", userController::createUser, Role.ADMIN);
            put("/reversActivtion/{id}", userController::reversActivation, Role.ADMIN);
            get("/all", userController::getAll, Role.ADMIN, Role.USER);
            get("/tenant/{tenantId}", userController::getByTenantId, Role.USER, Role.ADMIN);
            get("/{id}", userController::getById, Role.USER, Role.ADMIN);
            put("/{id}", userController::update, Role.USER, Role.ADMIN);
            delete("/{id}", userController::delete, Role.ADMIN);
            put("/{id}/admin", userController::setAdmin, Role.ADMIN);
            put("/{id}/employee", userController::setEmployee, Role.ADMIN);
            put("/{id}/roles", userController::updateUserRoles, Role.ADMIN);
            put("/{id}/custom-roles", userController::setCustomRoles, Role.ADMIN);
            post("/{id}/custom-roles/{roleId}", userController::addCustomRole, Role.ADMIN);
            delete("/{id}/custom-roles/{roleId}", userController::removeCustomRole, Role.ADMIN);
            put("/{id}/qualifications", qualificationController::replaceUserQualifications, Role.ADMIN);
            post("/{id}/qualifications/{qualificationId}", qualificationController::addUserQualification, Role.ADMIN);
            delete("/{id}/qualifications/{qualificationId}", qualificationController::removeUserQualification, Role.ADMIN);
        });
    }
}

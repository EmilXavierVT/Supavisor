package app.services.routeSecurity.routes;


import app.config.HibernateConfig;
import app.controller.*;
import app.services.routeSecurity.ISecurityController;
import app.services.routeSecurity.SecurityController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.security.RouteRole;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final EntityManagerFactory emf;
    ObjectMapper objectMapper = new ObjectMapper();
    private final ISecurityController securityController;

    public Routes() {
        this(HibernateConfig.getEntityManagerFactory());
    }


    public Routes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.securityController = new SecurityController(emf);
    }

    public EndpointGroup getRoutes() {

        app.controller.UserController userController = new app.controller.UserController(emf);
        TenantController tenantController = new TenantController(emf);
        RoleController roleController = new RoleController(emf);
        AssignmentController assignmentController = new AssignmentController(emf);
        ProjectController projectController = new ProjectController(emf);

        SystemController systemController = new SystemController();

        return () -> {
            get("/", ctx -> ctx.result("Hello Javalin World!"));
            get("/health", systemController::health, Role.ANYONE);

            path("/user", () -> {
                get("/all", userController::getAll, Role.ADMIN, Role.USER);
                post("/", userController::createUser, Role.ADMIN);
                get("/tenant/{tenantId}", userController::getByTenantId, Role.USER, Role.ADMIN);
                get("/{id}", userController::getById, Role.USER, Role.ADMIN);
                put("/{id}", userController::update, Role.USER, Role.ADMIN);
                delete("/{id}", userController::delete, Role.ADMIN);
                put("/{id}/admin", userController::setAdmin, Role.ADMIN);
                put("/{id}/employee", userController::setEmployee, Role.ADMIN);
                put("/{id}/cleaning-staff", userController::setCleaningStaff, Role.ADMIN);
                put("/{id}/cleaning-client", userController::setCleaningClient, Role.ADMIN);
                put("/{id}/subscriber", userController::setSubscriber, Role.ADMIN);
                put("/{id}/flex", userController::setFlex, Role.ADMIN);
                put("/{id}/roles", userController::updateUserRoles, Role.ADMIN);
                put("/{id}/custom-roles", userController::setCustomRoles, Role.ADMIN);
                post("/{id}/custom-roles/{roleId}", userController::addCustomRole, Role.ADMIN);
                delete("/{id}/custom-roles/{roleId}", userController::removeCustomRole, Role.ADMIN);
            });

            path("/tenant", () -> {
                get("/all", tenantController::getAll, Role.ADMIN, Role.USER);
                post("/", tenantController::create, Role.ADMIN);
                get("/{id}", tenantController::getById, Role.USER, Role.ADMIN);
                put("/{id}", tenantController::update, Role.ADMIN);
                delete("/{id}", tenantController::delete, Role.ADMIN);
            });

            path("/role", () -> {
                get("/tenant/{tenantId}", roleController::getRolesByTenant, Role.USER, Role.ADMIN);
                post("/", roleController::createRole, Role.ADMIN);
                delete("/{id}", roleController::deleteRole, Role.ADMIN);
            });

            path("user", () -> {
                put("/update", userController::update, Role.ADMIN);
                post("/create", userController::createUser, Role.ADMIN);
                put("/reversActivtion/{id}", userController::reversActivation, Role.ADMIN);
            });

            path("/assignment", () -> {
                get("/all", assignmentController::getAll, Role.ADMIN, Role.USER);
                get("/{id}", assignmentController::getById, Role.ADMIN, Role.USER);
                post("/", assignmentController::create, Role.ADMIN);
                put("/{id}", assignmentController::update, Role.ADMIN);
                patch("/{id}/deactivate", assignmentController::deactivate, Role.ADMIN);
                patch("/{id}/activate", assignmentController::activate, Role.ADMIN);
                delete("/{id}", assignmentController::delete, Role.ADMIN);
            });

            path("/project", () -> {
                get("/all", projectController::getAll, Role.ADMIN, Role.USER);
                get("/{id}/status-history", projectController::getStatusHistory, Role.ADMIN, Role.USER);
                get("/{id}", projectController::getById, Role.ADMIN, Role.USER);
                post("/", projectController::create, Role.ADMIN);
                put("/{id}", projectController::update, Role.ADMIN);
                delete("/{id}", projectController::delete, Role.ADMIN);
            });
        };
    }

    public EndpointGroup getRouteResource(String resourceName) {
        UserController userController = new UserController(emf);
        return switch (resourceName.toLowerCase()) {
            case "msg" -> () -> path("msg", () -> {
                ObjectNode on = objectMapper.createObjectNode();
                on.put("msg", "Hello World");
                get("hello", ctx -> ctx.json(on));
                post("echo", ctx -> ctx.result(ctx.body()));
            });
//
            case "auth" -> () -> path("auth", () -> {
                ObjectNode on = objectMapper.createObjectNode();
                on.put("msg","HELLO FROM THE RESTRICTED AREA");
                post("register", userController::create ); //TODO add admin only later after testing
                post("login", securityController::login );
                put("change-password", securityController::changePassword, Role.USER, Role.ADMIN, Role.EMPLOYEE, Role.CLEANING_STAFF, Role.CLEANING_CLIENT, Role.SUBSCRIBER, Role.FLEX);
                get("protected",ctx->ctx.json(on).status(200),Role.USER);
                post("token-validation", securityController::sendVerifiedTokenResponse);
            });
            default -> throw new IllegalArgumentException("Unknown resource name: " + resourceName);
        };
    }

    public enum Role implements RouteRole {
        ANYONE,USER,ADMIN,EMPLOYEE,CLEANING_STAFF,CLEANING_CLIENT,SUBSCRIBER,FLEX
    }

}

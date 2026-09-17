package app.services.routeSecurity.routes;


import app.config.HibernateConfig;
import app.controller.SystemController;
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

        UserRoutes userRoutes = new UserRoutes(emf);
        TenantRoutes tenantRoutes = new TenantRoutes(emf);

        SystemController systemController = new SystemController();

        return () -> {
            get("/", ctx -> ctx.result("Hello Javalin World!"));
            get("/health", systemController::health, Role.ANYONE);


            path("/user", () -> {
                get("/all", userRoutes::getAll, Role.ADMIN, Role.USER);
                post("/", userRoutes::create, Role.ADMIN);
                get("/tenant/{tenantId}", userRoutes::getByTenantId, Role.USER, Role.ADMIN);
                get("/{id}", userRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", userRoutes::update, Role.USER, Role.ADMIN);
                delete("/{id}", userRoutes::delete, Role.ADMIN);
                put("/{id}/admin", userRoutes::setAdmin, Role.ADMIN);
                put("/{id}/employee", userRoutes::setEmployee, Role.ADMIN);
                put("/{id}/cleaning-staff", userRoutes::setCleaningStaff, Role.ADMIN);
                put("/{id}/cleaning-client", userRoutes::setCleaningClient, Role.ADMIN);
                put("/{id}/subscriber", userRoutes::setSubscriber, Role.ADMIN);
                put("/{id}/flex", userRoutes::setFlex, Role.ADMIN);
            });

            path("/tenant", () -> {
                get("/all", tenantRoutes::getAll, Role.ADMIN, Role.USER);
                post("/", tenantRoutes::create, Role.ADMIN);
                get("/{id}", tenantRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", tenantRoutes::update, Role.ADMIN);
                delete("/{id}", tenantRoutes::delete, Role.ADMIN);
            });

        };
    }

    public EndpointGroup getRouteResource(String resourceName) {
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
                post("register", securityController::register );
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

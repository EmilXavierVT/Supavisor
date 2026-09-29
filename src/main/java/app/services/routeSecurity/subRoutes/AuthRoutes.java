package app.services.routeSecurity.subRoutes;

import app.controller.SecurityController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.*;

public class AuthRoutes {

    private final SecurityController securityController;

    public AuthRoutes(EntityManagerFactory emf) {
        this.securityController = new SecurityController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("auth", () -> {
            post("register", securityController::register, Role.ANYONE);
            post("login", securityController::login);
            get("protected", ctx -> ctx.status(200).json(Map.of("msg", "HELLO FROM THE RESTRICTED AREA")), Role.USER);
            post("token-validation", securityController::sendVerifiedTokenResponse);
        });
    }
}

package app.services.routeSecurity.subRoutes;

import app.controller.SystemController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;

import static io.javalin.apibuilder.ApiBuilder.*;

public class SystemRoutes {

    private final SystemController systemController;

    public SystemRoutes() {
        this.systemController = new SystemController();
    }

    public EndpointGroup getRoutes() {
        return () -> path("system", () -> {
            get("/", ctx -> ctx.result("Hello Javalin World!"), Role.ANYONE);
            get("health", systemController::health, Role.ANYONE);
            get("hello", systemController::hello, Role.ANYONE);
            post("echo", systemController::echo, Role.ANYONE);
        });
    }
}

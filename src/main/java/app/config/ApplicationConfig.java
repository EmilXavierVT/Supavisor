package app.config;

import app.exceptions.ApiException;
import app.services.routeSecurity.ISecurityController;
import app.services.routeSecurity.SecurityController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ApplicationConfig {




    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    private final ISecurityController securityController;

    private final List<EndpointGroup> routes = new ArrayList<>();
    private final List<Consumer<JavalinConfig>> configSteps = new ArrayList<>();

    private Javalin app;

    public ApplicationConfig() {
        this.securityController = new SecurityController();
        configSteps.add(this::applyBaseConfig);
    }


    public ApplicationConfig(EntityManagerFactory emf) {
        this.securityController = new SecurityController(emf);
        configSteps.add(this::applyBaseConfig);
    }

    public ApplicationConfig route(EndpointGroup route) {
        routes.add(route);
        return this;
    }

    public ApplicationConfig routeOverview() {
        configSteps.add(config ->
                config.bundledPlugins.enableRouteOverview("/routes")
        );
        return this;
    }
    public ApplicationConfig cors() {
        configSteps.add(config -> {
            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(rule -> {
                        rule.anyHost();
                    })
            );
            config.bundledPlugins.enableHttpAllowedMethodsOnRoutes();
        });
        return this;
    }

    public ApplicationConfig security() {
        configSteps.add(config -> {
            config.routes.beforeMatched(securityController::authenticate);
            config.routes.beforeMatched(securityController::authorize);
        });
        return this;
    }

    public ApplicationConfig apiExceptions() {
        configSteps.add(config ->
                config.routes.exception(ApiException.class, (e, ctx) -> {
                    int statusCode = e.getCode();
                    ObjectNode body = jsonMapper.createObjectNode()
                            .put("status", statusCode)
                            .put("msg", e.getMessage());
                    ctx.status(statusCode).json(body);
                })
        );
        return this;
    }

    public ApplicationConfig exceptions() {
        configSteps.add(config ->
                config.routes.exception(Exception.class, (e, ctx) -> {
                    ObjectNode body = jsonMapper.createObjectNode()
                            .put("status", 500)
                            .put("msg", "An unexpected error occurred");
                    logger.error("Unhandled exception", e);
                    ctx.status(500).json(body);
                })
        );
        return this;
    }

    public ApplicationConfig notFound() {
        configSteps.add(config ->
                config.routes.error(404, ctx -> {
                    String message = ctx.attribute("msg");
                    ObjectNode body = jsonMapper.createObjectNode()
                            .put("msg", message == null ? "Not found" : message);
                    ctx.json(body);
                })
        );
        return this;
    }

    public ApplicationConfig requestLogger() {
        configSteps.add(config ->
                config.routes.before(ctx ->
                        ctx.req().getHeaderNames().asIterator().forEachRemaining(System.out::println)
                )
        );
        return this;
    }

    public Javalin start(int port) {
        app = Javalin.create(config -> {
            for (Consumer<JavalinConfig> step : configSteps) {
                step.accept(config);
            }
            for (EndpointGroup route : routes) {
                config.routes.apiBuilder(route);
            }
        });

        app.start(port);
        return app;
    }

    public void stopServer() {
        if (app != null) {
            app.stop();
            app = null;
        }
    }

    private void applyBaseConfig(JavalinConfig config) {
        config.http.defaultContentType = "application/json";
        config.router.contextPath = "/api";

        config.bundledPlugins.enableDevLogging();
//        config.bundledPlugins.enableRouteOverview("/routes", Role.ANYONE);

//        config.staticFiles.add(files -> {
//            files.hostedPath = "/";
//            files.directory = "/public";
//            files.location = Location.CLASSPATH;
//        });
        config.router.contextPath = "/api";
        config.jsonMapper(new JavalinJackson().updateMapper(objectMapper -> {
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            config.events.serverStarted(() ->
                    System.out.println("Javalin started on /api")
            );

            config.events.serverStopped(() ->
                    System.out.println("Javalin stopped")
            );
        }));

    }





//    private final Routes routes;
//
//    public ApplicationConfig(Routes routes) {
//        this.routes = routes;
//    }

//    public void configuration(JavalinConfig config){
//        config.bundledPlugins.enableRouteOverview("/routes");
//        config.router.contextPath = "/api";
//        config.jsonMapper(new JavalinJackson().updateMapper(objectMapper -> {
//            objectMapper.registerModule(new JavaTimeModule());
//            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        }));
//
//        config.routes.apiBuilder(routes.getRoutes());
//        config.routes.exception(ValidationException.class, (e, ctx) -> {
//            ctx.status(HttpStatus.BAD_REQUEST);
//            ctx.json(Map.of(
//                    "message", "Path parameter must be a number",
//                    "path", ctx.path()
//            ));
//        });
//        config.routes.exception(MismatchedInputException.class, (e, ctx) -> {
//            ctx.status(HttpStatus.BAD_REQUEST).json("Invalid or Empty Request Body");
//        });
//    }
//
//    public Javalin startServer(int port) {
//        var app = Javalin.create(this::configuration);
//        app.start(port);
//        return app;
//    }


    public void stopServer(Javalin app) {
        app.stop();
    }
}

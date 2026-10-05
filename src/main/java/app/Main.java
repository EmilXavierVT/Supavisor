package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.services.entityServices.AssignmentAutoAcceptanceService;
import app.services.routeSecurity.RoutePackage;
import app.services.scheduling.AssignmentAutoAcceptanceJob;
import jakarta.persistence.EntityManagerFactory;

public class Main {
    public static void main(String[] args) {
        int port = resolvePort(args);
        EntityManagerFactory entityManagerFactory = HibernateConfig.getEntityManagerFactory();
        RoutePackage routes = new RoutePackage(entityManagerFactory);

        new ApplicationConfig(entityManagerFactory)
                .cors()
                .apiExceptions()
                .exceptions()
                .notFound()
                .security()
                .route(routes.getRoutes())
                .start(port);

        AssignmentAutoAcceptanceJob autoAcceptanceJob = new AssignmentAutoAcceptanceJob(
                new AssignmentAutoAcceptanceService(entityManagerFactory)
        );
        autoAcceptanceJob.start();
        Runtime.getRuntime().addShutdownHook(new Thread(autoAcceptanceJob::close));
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Integer.parseInt(args[0]);
        }

        String port = System.getenv("PORT");
        if (port != null && !port.isBlank()) {
            return Integer.parseInt(port);
        }

        return 7070;
    }
}

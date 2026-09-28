package app;

import app.config.ApplicationConfig;
import app.services.routeSecurity.RoutePackage;

public class Main {
    public static void main(String[] args) {
        int port = resolvePort(args);
        RoutePackage routes = new RoutePackage();

        new ApplicationConfig()
                .cors()
                .apiExceptions()
                .exceptions()
                .notFound()
                .security()
                .route(routes.getRoutes())
                .start(port);
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

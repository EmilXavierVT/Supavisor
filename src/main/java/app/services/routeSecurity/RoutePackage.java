package app.services.routeSecurity;

import app.config.HibernateConfig;
import app.services.routeSecurity.subRoutes.*;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

public class RoutePackage {

    private final EntityManagerFactory emf;

    public RoutePackage() {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public RoutePackage(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public EndpointGroup getRoutes() {
        return () -> {
            new SystemRoutes().getRoutes().addEndpoints();
            new AuthRoutes(emf).getRoutes().addEndpoints();
            new UserRoutes(emf).getRoutes().addEndpoints();
            new TenantRoutes(emf).getRoutes().addEndpoints();
            new RoleRoutes(emf).getRoutes().addEndpoints();
            new ProjectRoutes(emf).getRoutes().addEndpoints();
            new AssignmentRoutes(emf).getRoutes().addEndpoints();
        };
    }
}

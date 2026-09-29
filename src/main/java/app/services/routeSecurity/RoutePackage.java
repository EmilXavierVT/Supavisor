package app.services.routeSecurity;

import app.config.HibernateConfig;
import app.services.routeSecurity.routes.EconomicCustomerRoutes;
import app.services.routeSecurity.routes.EconomicProductRoutes;
import app.services.routeSecurity.subRoutes.*;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

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
            addEconomicRoutes();
        };
    }

    private void addEconomicRoutes() {
        EconomicCustomerRoutes customerRoutes = new EconomicCustomerRoutes(emf);
        EconomicProductRoutes productRoutes = new EconomicProductRoutes(emf);

        path("/economic", () -> {
            path("/customers", () -> {
                get("/", customerRoutes::listEconomicCustomers, Role.ADMIN, Role.USER);
                get("/{customerNumber}", customerRoutes::getEconomicCustomer, Role.ADMIN, Role.USER);
                post("/", customerRoutes::createCustomer, Role.ADMIN);
            });
            path("/products", () -> {
                get("/", productRoutes::listProducts, Role.ADMIN, Role.USER);
                get("/{productNumber}", productRoutes::getProduct, Role.ADMIN, Role.USER);
                post("/", productRoutes::createProduct, Role.ADMIN);
                put("/{productNumber}", productRoutes::updateProduct, Role.ADMIN);
            });
        });
    }
}

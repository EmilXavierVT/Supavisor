package app.services.routeSecurity.subRoutes;

import app.controller.EconomicCustomerController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class EconomicCustomerRoutes {
    private final EconomicCustomerController customerController;

    public EconomicCustomerRoutes(EntityManagerFactory emf) {
        this.customerController = new EconomicCustomerController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("economic/customers", () -> {
            get("/", customerController::listEconomicCustomers, Role.ADMIN);
            get("/{customerNumber}", customerController::getEconomicCustomer, Role.ADMIN);
            post("/", customerController::createCustomer, Role.ADMIN);
        });
    }
}

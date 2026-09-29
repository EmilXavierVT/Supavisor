package app.services.routeSecurity.routes;

import app.dto.CreateCustomerRequest;
import app.services.entityServices.EconomicCustomerService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class EconomicCustomerRoutes {
    private final EconomicCustomerService customerService;

    public EconomicCustomerRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.customerService = new EconomicCustomerService(emf);
    }

    public void listEconomicCustomers(Context ctx) {
        int pageSize = ctx.queryParamAsClass("pagesize", Integer.class).getOrDefault(20);
        int skipPages = ctx.queryParamAsClass("skippages", Integer.class).getOrDefault(0);
        ctx.json(customerService.listCustomers(pageSize, skipPages));
    }

    public void getEconomicCustomer(Context ctx) {
        Integer customerNumber = ctx.pathParamAsClass("customerNumber", Integer.class).get();
        ctx.json(customerService.getCustomer(customerNumber));
    }

    public void createCustomer(Context ctx) {
        CreateCustomerRequest request = ctx.bodyValidator(CreateCustomerRequest.class).get();
        ctx.status(201).json(customerService.createCustomer(request));
    }
}

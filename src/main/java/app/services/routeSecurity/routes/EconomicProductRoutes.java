package app.services.routeSecurity.routes;

import app.dto.CreateProductRequest;
import app.dto.UpdateProductRequest;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.entityServices.EconomicProductService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class EconomicProductRoutes {
    private final EconomicProductService productService;

    public EconomicProductRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productService = new EconomicProductService(emf);
    }

    public void listProducts(Context ctx) {
        int pageSize = ctx.queryParamAsClass("pagesize", Integer.class).getOrDefault(100);
        int skipPages = ctx.queryParamAsClass("skippages", Integer.class).getOrDefault(0);
        ctx.json(productService.listProducts(callerTenantId(ctx), pageSize, skipPages));
    }

    public void getProduct(Context ctx) {
        String productNumber = ctx.pathParam("productNumber");
        ctx.json(productService.getProduct(callerTenantId(ctx), productNumber));
    }

    public void createProduct(Context ctx) {
        CreateProductRequest request = ctx.bodyValidator(CreateProductRequest.class).get();
        ctx.status(201).json(productService.createProduct(request, callerTenantId(ctx)));
    }

    public void updateProduct(Context ctx) {
        String productNumber = ctx.pathParam("productNumber");
        UpdateProductRequest request = ctx.bodyValidator(UpdateProductRequest.class).get();
        ctx.json(productService.updateProduct(callerTenantId(ctx), productNumber, request));
    }

    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }
}

package app.services.routeSecurity.subRoutes;

import app.controller.EconomicProductController;
import app.services.routeSecurity.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

public class EconomicProductRoutes {
    private final EconomicProductController productController;

    public EconomicProductRoutes(EntityManagerFactory emf) {
        this.productController = new EconomicProductController(emf);
    }

    public EndpointGroup getRoutes() {
        return () -> path("economic/products", () -> {
            get("/", productController::listProducts, Role.ADMIN);
            get("/{productNumber}", productController::getProduct, Role.ADMIN);
            post("/", productController::createProduct, Role.ADMIN);
            put("/{productNumber}", productController::updateProduct, Role.ADMIN);
        });
    }
}

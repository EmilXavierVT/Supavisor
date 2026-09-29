package app.services.economic;

import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.exceptions.ApiException;
import app.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EconomicProductClientIntegrationTest {

    @Test
    void retrievesProductsFromEconomicApi() {
        assumeTrue(isConfigured("ECONOMIC_APP_SECRET_TOKEN"), "ECONOMIC_APP_SECRET_TOKEN is not configured");
        assumeTrue(isConfigured("ECONOMIC_AGREEMENT_GRANT_TOKEN"), "ECONOMIC_AGREEMENT_GRANT_TOKEN is not configured");

        EconomicProductClient client = new EconomicProductClient();
        EconomicProductListResponse products = listProductsOrSkip(client);

        assertNotNull(products);
        assertNotNull(products.getCollection());
        assumeFalse(products.getCollection().isEmpty(), "No e-conomic products were returned");

        String productNumber = products.getCollection().get(0).getProductNumber();
        assertNotNull(productNumber);

        EconomicProductResponse product = getProductOrSkip(client, productNumber);

        assertEquals(productNumber, product.getProductNumber());
        assertNotNull(product.getName());
    }

    private boolean isConfigured(String key) {
        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) return true;

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) return true;

        try {
            String configValue = Utils.getPropertyValue(key, "config.properties");
            return configValue != null && !configValue.isBlank();
        } catch (ApiException e) {
            return false;
        }
    }

    private EconomicProductListResponse listProductsOrSkip(EconomicProductClient client) {
        try {
            return client.listProducts(100, 0);
        } catch (ApiException e) {
            assumeTrue(e.getCode() != 401 && e.getCode() != 403, "Configured e-conomic credentials cannot retrieve products");
            throw e;
        }
    }

    private EconomicProductResponse getProductOrSkip(EconomicProductClient client, String productNumber) {
        try {
            return client.getProduct(productNumber);
        } catch (ApiException e) {
            assumeTrue(e.getCode() != 401 && e.getCode() != 403, "Configured e-conomic credentials cannot retrieve products");
            throw e;
        }
    }
}

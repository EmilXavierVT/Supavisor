package app.services.economic;

import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerResponse;
import app.exceptions.ApiException;
import app.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class EconomicCustomerClientIntegrationTest {

    @Test
    void retrievesCustomersFromEconomicApi() {
        assumeTrue(isConfigured("ECONOMIC_APP_SECRET_TOKEN"), "ECONOMIC_APP_SECRET_TOKEN is not configured");
        assumeTrue(isConfigured("ECONOMIC_AGREEMENT_GRANT_TOKEN"), "ECONOMIC_AGREEMENT_GRANT_TOKEN is not configured");

        EconomicCustomerClient client = new EconomicCustomerClient();

        EconomicCustomerListResponse customers = listCustomersOrSkip(client);

        assertNotNull(customers);
        assertNotNull(customers.getCollection());
        assumeFalse(customers.getCollection().isEmpty(), "No e-conomic customers were returned");

        Integer customerNumber = customers.getCollection().get(0).getCustomerNumber();
        assertNotNull(customerNumber);

        EconomicCustomerResponse customer = getCustomerOrSkip(client, customerNumber);
        System.out.println("First customer retrieved from e-conomic: Customer number " + customerNumber + ", name " + customer.getName() + ".");

        assertEquals(customerNumber, customer.getCustomerNumber());
        assertNotNull(customer.getName());
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

    private EconomicCustomerListResponse listCustomersOrSkip(EconomicCustomerClient client) {
        try {
            return client.listCustomers(20, 0);
        } catch (ApiException e) {
            assumeTrue(e.getCode() != 401 && e.getCode() != 403, "Configured e-conomic credentials cannot retrieve customers");
            throw e;
        }
    }

    private EconomicCustomerResponse getCustomerOrSkip(EconomicCustomerClient client, Integer customerNumber) {
        try {
            return client.getCustomer(customerNumber);
        } catch (ApiException e) {
            assumeTrue(e.getCode() != 401 && e.getCode() != 403, "Configured e-conomic credentials cannot retrieve customers");
            throw e;
        }
    }
}

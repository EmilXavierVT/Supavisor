package app.dto;

import app.services.routeSecurity.ObjectMapperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EconomicCustomerDTOTest {
    private final ObjectMapper mapper = ObjectMapperService.getMapper();

    @Test
    void serializesEconomicCustomerRequestWithNestedReferences() throws Exception {
        EconomicCustomerRequest request = new EconomicCustomerRequest();
        request.setName("Example Company ApS");
        request.setCurrency("DKK");
        request.setCustomerGroup(new EconomicCustomerRequest.CustomerGroup(1));
        request.setPaymentTerms(new EconomicCustomerRequest.PaymentTerms(2));
        request.setVatZone(new EconomicCustomerRequest.VatZone(3));

        String json = mapper.writeValueAsString(request);

        assertTrue(json.contains("\"customerGroupNumber\":1"));
        assertTrue(json.contains("\"paymentTermsNumber\":2"));
        assertTrue(json.contains("\"vatZoneNumber\":3"));
        assertFalse(json.contains("AppSecretToken"));
        assertFalse(json.contains("AgreementGrantToken"));
    }

    @Test
    void deserializesEconomicCustomerListResponse() throws Exception {
        String json = "{\"collection\":[{\"customerNumber\":42,\"name\":\"Example\"}],\"pagination\":{\"maxPageSizeAllowed\":1000}}";

        EconomicCustomerListResponse response = mapper.readValue(json, EconomicCustomerListResponse.class);

        assertEquals(1, response.getCollection().size());
        assertEquals(42, response.getCollection().get(0).getCustomerNumber());
        assertEquals(1000, response.getPagination().path("maxPageSizeAllowed").asInt());
    }
}

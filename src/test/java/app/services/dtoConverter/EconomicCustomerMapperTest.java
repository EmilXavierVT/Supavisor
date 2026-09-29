package app.services.dtoConverter;

import app.dto.CreateCustomerRequest;
import app.dto.EconomicCustomerRequest;
import app.dto.EconomicCustomerResponse;
import app.entities.Customer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomicCustomerMapperTest {
    private final EconomicCustomerMapper mapper = new EconomicCustomerMapper();

    @Test
    void mapsCreateRequestEntityEconomicRequestAndResponseExplicitly() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Example");
        request.setEmail("billing@example.com");
        request.setPostalCode("2100");
        request.setCurrency("DKK");
        request.setCustomerGroupNumber(1);
        request.setPaymentTermsNumber(2);
        request.setVatZoneNumber(3);

        Customer customer = mapper.fromCreateRequest(request, 1L);
        EconomicCustomerRequest economicRequest = mapper.toEconomicRequest(customer, request);
        EconomicCustomerResponse economicResponse = new EconomicCustomerResponse();
        economicResponse.setCustomerNumber(99);
        mapper.applyEconomicResponse(customer, economicResponse);

        assertEquals("Example", customer.getName());
        assertEquals("2100", economicRequest.getZip());
        assertEquals(1, economicRequest.getCustomerGroup().getCustomerGroupNumber());
        assertEquals(2, economicRequest.getPaymentTerms().getPaymentTermsNumber());
        assertEquals(3, economicRequest.getVatZone().getVatZoneNumber());
        assertEquals(99, mapper.toResponse(customer).getEconomicCustomerNumber());
    }
}

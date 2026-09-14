package app.services.dtoConverter;

import app.dto.CreateCustomerRequest;
import app.dto.CustomerResponse;
import app.dto.EconomicCustomerRequest;
import app.dto.EconomicCustomerResponse;
import app.entities.Customer;

public class EconomicCustomerMapper {
    public Customer fromCreateRequest(CreateCustomerRequest request) {
        if (request == null) return null;
        return new Customer(
                null,
                request.getName(),
                request.getEmail(),
                request.getAddress(),
                request.getPostalCode(),
                request.getCity(),
                request.getCountry(),
                request.getCorporateIdentificationNumber(),
                request.getCurrency(),
                null,
                request.getIdempotencyKey(),
                null,
                null
        );
    }

    public EconomicCustomerRequest toEconomicRequest(Customer customer, CreateCustomerRequest request) {
        if (customer == null || request == null) return null;
        EconomicCustomerRequest economicRequest = new EconomicCustomerRequest();
        economicRequest.setName(customer.getName());
        economicRequest.setEmail(customer.getEmail());
        economicRequest.setAddress(customer.getAddress());
        economicRequest.setZip(customer.getPostalCode());
        economicRequest.setCity(customer.getCity());
        economicRequest.setCountry(customer.getCountry());
        economicRequest.setCorporateIdentificationNumber(customer.getCorporateIdentificationNumber());
        economicRequest.setCurrency(customer.getCurrency());
        economicRequest.setCustomerGroup(new EconomicCustomerRequest.CustomerGroup(request.getCustomerGroupNumber()));
        economicRequest.setPaymentTerms(new EconomicCustomerRequest.PaymentTerms(request.getPaymentTermsNumber()));
        economicRequest.setVatZone(new EconomicCustomerRequest.VatZone(request.getVatZoneNumber()));
        return economicRequest;
    }

    public void applyEconomicResponse(Customer customer, EconomicCustomerResponse response) {
        if (customer == null || response == null) return;
        customer.setEconomicCustomerNumber(response.getCustomerNumber());
    }

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) return null;
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getAddress(),
                customer.getPostalCode(),
                customer.getCity(),
                customer.getCountry(),
                customer.getCorporateIdentificationNumber(),
                customer.getCurrency(),
                customer.getEconomicCustomerNumber(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}

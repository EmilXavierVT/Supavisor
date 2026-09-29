package app.services.entityServices;

import app.dao.CustomerDAO;
import app.dto.CreateCustomerRequest;
import app.dto.CustomerResponse;
import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerRequest;
import app.dto.EconomicCustomerResponse;
import app.entities.Customer;
import app.exceptions.ApiException;
import app.services.dtoConverter.EconomicCustomerMapper;
import app.services.economic.EconomicCustomerClient;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EconomicCustomerService {
    private static final Logger logger = LoggerFactory.getLogger(EconomicCustomerService.class);
    private final CustomerDAO customerDAO;
    private final EconomicCustomerMapper mapper;
    private final EconomicCustomerClient client;

    public EconomicCustomerService(EntityManagerFactory emf) {
        this(new CustomerDAO(emf), new EconomicCustomerMapper(), new EconomicCustomerClient());
    }

    public EconomicCustomerService(CustomerDAO customerDAO, EconomicCustomerMapper mapper, EconomicCustomerClient client) {
        this.customerDAO = customerDAO;
        this.mapper = mapper;
        this.client = client;
    }

    public EconomicCustomerListResponse listCustomers(int pageSize, int skipPages) {
        int safePageSize = validatePageSize(pageSize);
        int safeSkipPages = validateSkipPages(skipPages);
        return client.listCustomers(safePageSize, safeSkipPages);
    }

    public EconomicCustomerResponse getCustomer(Integer customerNumber) {
        if (customerNumber == null || customerNumber < 1) {
            throw new ApiException(400, "customerNumber must be a positive number");
        }
        return client.getCustomer(customerNumber);
    }

    public CustomerResponse createCustomer(CreateCustomerRequest request, Long tenantId) {
        validateTenantId(tenantId);
        validateCreateRequest(request);

        Customer existing = customerDAO.findByIdempotencyKey(tenantId, request.getIdempotencyKey());
        if (existing != null && existing.getEconomicCustomerNumber() != null) {
            return mapper.toResponse(existing);
        }

        Customer customer = existing == null ? customerDAO.save(mapper.fromCreateRequest(request, tenantId)) : existing;
        EconomicCustomerRequest economicRequest = mapper.toEconomicRequest(customer, request);
        EconomicCustomerResponse economicResponse = client.createCustomer(economicRequest, customer.getIdempotencyKey());

        if (economicResponse.getCustomerNumber() == null) {
            logger.warn("operation=createEconomicCustomer localCustomerId={} upstreamStatus=201 economicLogId=missing", customer.getId());
            throw new ApiException(502, "e-conomic did not return a customer number");
        }
        Customer mappedCustomer = customerDAO.findByEconomicCustomerNumber(tenantId, economicResponse.getCustomerNumber());
        if (mappedCustomer != null && !mappedCustomer.getId().equals(customer.getId())) {
            throw new ApiException(409, "e-conomic customer number is already mapped");
        }

        mapper.applyEconomicResponse(customer, economicResponse);
        Customer saved = customerDAO.save(customer);
        return mapper.toResponse(saved);
    }

    private void validateTenantId(Long tenantId) {
        if (tenantId == null) throw new ApiException(401, "Not authenticated or tenantId missing from token");
    }

    private int validatePageSize(int pageSize) {
        if (pageSize < 1) return 20;
        if (pageSize > 1000) return 1000;
        return pageSize;
    }

    private int validateSkipPages(int skipPages) {
        if (skipPages < 0) return 0;
        return skipPages;
    }

    private void validateCreateRequest(CreateCustomerRequest request) {
        if (request == null) throw new ApiException(400, "Request body is required");
        if (isBlank(request.getName())) throw new ApiException(400, "name is required");
        if (isBlank(request.getCurrency())) throw new ApiException(400, "currency is required");
        if (request.getCustomerGroupNumber() == null) throw new ApiException(400, "customerGroupNumber is required");
        if (request.getPaymentTermsNumber() == null) throw new ApiException(400, "paymentTermsNumber is required");
        if (request.getVatZoneNumber() == null) throw new ApiException(400, "vatZoneNumber is required");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

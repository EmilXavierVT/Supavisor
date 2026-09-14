package app.services.entityServices;

import app.config.TestEntityManagerFactory;
import app.dao.CustomerDAO;
import app.dto.CreateCustomerRequest;
import app.dto.EconomicCustomerRequest;
import app.dto.EconomicCustomerResponse;
import app.entities.Customer;
import app.exceptions.ApiException;
import app.services.dtoConverter.EconomicCustomerMapper;
import app.services.economic.EconomicCustomerClient;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class EconomicCustomerServiceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_economic_service_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) emf.close();
    }

    @Test
    void createsCustomerAndPersistsReturnedEconomicCustomerNumber() {
        CustomerDAO dao = new CustomerDAO(emf);
        FakeClient client = new FakeClient(7001);
        EconomicCustomerService service = new EconomicCustomerService(dao, new EconomicCustomerMapper(), client);

        Long localId = service.createCustomer(validRequest("service-idem-1")).getId();

        Customer saved = dao.findById(localId);
        assertEquals(7001, saved.getEconomicCustomerNumber());
        assertEquals("service-idem-1", client.lastIdempotencyKey);
    }

    @Test
    void reusesCompletedMappingForSameIdempotencyKey() {
        CustomerDAO dao = new CustomerDAO(emf);
        FakeClient client = new FakeClient(7002);
        EconomicCustomerService service = new EconomicCustomerService(dao, new EconomicCustomerMapper(), client);

        service.createCustomer(validRequest("service-idem-2"));
        service.createCustomer(validRequest("service-idem-2"));

        assertEquals(1, client.createCalls);
    }

    @Test
    void preventsDuplicateEconomicCustomerNumberMapping() {
        CustomerDAO dao = new CustomerDAO(emf);
        dao.save(new Customer(null, "Existing", null, null, null, null, null, null, "DKK", 7003, "existing-idem", null, null));
        EconomicCustomerService service = new EconomicCustomerService(dao, new EconomicCustomerMapper(), new FakeClient(7003));

        ApiException exception = assertThrows(ApiException.class, () -> service.createCustomer(validRequest("service-idem-3")));

        assertEquals(409, exception.getCode());
    }

    @Test
    void rejectsMissingRequiredFields() {
        EconomicCustomerService service = new EconomicCustomerService(new CustomerDAO(emf), new EconomicCustomerMapper(), new FakeClient(1));
        CreateCustomerRequest request = validRequest("service-idem-4");
        request.setName(null);

        ApiException exception = assertThrows(ApiException.class, () -> service.createCustomer(request));

        assertEquals(400, exception.getCode());
    }

    private CreateCustomerRequest validRequest(String idempotencyKey) {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Example Company ApS");
        request.setEmail("billing@example.com");
        request.setAddress("Example Street 10");
        request.setPostalCode("2100");
        request.setCity("Copenhagen");
        request.setCountry("Denmark");
        request.setCorporateIdentificationNumber("12345678");
        request.setCurrency("DKK");
        request.setCustomerGroupNumber(1);
        request.setPaymentTermsNumber(1);
        request.setVatZoneNumber(1);
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }

    private static class FakeClient extends EconomicCustomerClient {
        private final int customerNumber;
        private int createCalls;
        private String lastIdempotencyKey;

        private FakeClient(int customerNumber) {
            super(HttpClient.newHttpClient(), "http://localhost", "app", "agreement");
            this.customerNumber = customerNumber;
        }

        @Override
        public EconomicCustomerResponse createCustomer(EconomicCustomerRequest customerRequest, String idempotencyKey) {
            createCalls++;
            lastIdempotencyKey = idempotencyKey;
            EconomicCustomerResponse response = new EconomicCustomerResponse();
            response.setCustomerNumber(customerNumber);
            return response;
        }
    }
}

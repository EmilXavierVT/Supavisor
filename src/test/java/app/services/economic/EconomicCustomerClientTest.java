package app.services.economic;

import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerRequest;
import app.dto.EconomicCustomerResponse;
import app.exceptions.ApiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EconomicCustomerClientTest {
    private HttpServer server;
    private List<HttpExchange> exchanges;
    private String responseBody;
    private int responseStatus;

    @BeforeEach
    void setUp() throws Exception {
        exchanges = new ArrayList<>();
        responseStatus = 200;
        responseBody = "{}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/customers", exchange -> {
            exchanges.add(exchange);
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void listsCustomersWithPaginationAndAuthenticationHeaders() {
        responseBody = "{\"collection\":[{\"customerNumber\":10,\"name\":\"Example\"}],\"pagination\":{\"skipPages\":2}}";
        EconomicCustomerClient client = client();

        EconomicCustomerListResponse response = client.listCustomers(50, 2);

        assertEquals(10, response.getCollection().get(0).getCustomerNumber());
        assertEquals("/customers?pagesize=50&skippages=2", exchanges.get(0).getRequestURI().toString());
        assertHeaders(exchanges.get(0));
    }

    @Test
    void retrievesOneCustomer() {
        responseBody = "{\"customerNumber\":11,\"name\":\"Example\"}";

        EconomicCustomerResponse response = client().getCustomer(11);

        assertEquals(11, response.getCustomerNumber());
        assertEquals("/customers/11", exchanges.get(0).getRequestURI().toString());
    }

    @Test
    void createsCustomerWithStableIdempotencyKey() {
        responseBody = "{\"customerNumber\":12,\"name\":\"Example\"}";
        EconomicCustomerRequest request = new EconomicCustomerRequest();
        request.setName("Example");
        request.setCurrency("DKK");

        EconomicCustomerResponse response = client().createCustomer(request, "idem-key");

        assertEquals(12, response.getCustomerNumber());
        assertEquals("idem-key", exchanges.get(0).getRequestHeaders().getFirst("Idempotency-Key"));
        assertHeaders(exchanges.get(0));
    }

    @Test
    void convertsNotFoundValidationAuthRateLimitAndUpstreamErrorsSafely() {
        assertStatus(400, 400);
        assertStatus(401, 401);
        assertStatus(403, 403);
        assertStatus(404, 404);
        assertStatus(429, 429);
        assertStatus(500, 502);
    }

    @Test
    void doesNotExposeCredentialsInErrors() {
        responseStatus = 401;
        responseBody = "{\"message\":\"bad secret-token\",\"logId\":\"abc\"}";

        ApiException exception = assertThrows(ApiException.class, () -> client().listCustomers(20, 0));

        assertFalse(exception.getMessage().contains("app-secret"));
        assertFalse(exception.getMessage().contains("agreement-token"));
    }

    @Test
    void convertsNetworkFailureToBadGateway() {
        EconomicCustomerClient client = new EconomicCustomerClient(HttpClient.newHttpClient(), "http://localhost:1", "app", "agreement");

        ApiException exception = assertThrows(ApiException.class, () -> client.listCustomers(20, 0));

        assertEquals(502, exception.getCode());
    }

    private void assertStatus(int upstreamStatus, int applicationStatus) {
        responseStatus = upstreamStatus;
        responseBody = "{\"message\":\"upstream details\",\"logId\":\"log-1\"}";

        ApiException exception = assertThrows(ApiException.class, () -> client().listCustomers(20, 0));

        assertEquals(applicationStatus, exception.getCode());
    }

    private EconomicCustomerClient client() {
        return new EconomicCustomerClient(HttpClient.newHttpClient(), "http://localhost:" + server.getAddress().getPort(), "app-secret", "agreement-token");
    }

    private void assertHeaders(HttpExchange exchange) {
        assertEquals("app-secret", exchange.getRequestHeaders().getFirst("X-AppSecretToken"));
        assertEquals("agreement-token", exchange.getRequestHeaders().getFirst("X-AgreementGrantToken"));
        assertEquals("application/json", exchange.getRequestHeaders().getFirst("Accept"));
    }
}

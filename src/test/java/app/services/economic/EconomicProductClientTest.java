package app.services.economic;

import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductRequest;
import app.dto.EconomicProductResponse;
import app.exceptions.ApiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EconomicProductClientTest {
    private HttpServer server;
    private List<HttpExchange> exchanges;
    private List<String> requestBodies;
    private String responseBody;
    private int responseStatus;

    @BeforeEach
    void setUp() throws Exception {
        exchanges = new ArrayList<>();
        requestBodies = new ArrayList<>();
        responseStatus = 200;
        responseBody = "{}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/products", exchange -> {
            exchanges.add(exchange);
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
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
    void listsProductsWithPaginationAndAuthenticationHeaders() {
        responseBody = "{\"collection\":[{\"productNumber\":\"P-1\",\"name\":\"Example\"}],\"pagination\":{\"skipPages\":2}}";

        EconomicProductListResponse response = client().listProducts(50, 2);

        assertEquals("P-1", response.getCollection().get(0).getProductNumber());
        assertEquals("/products?pagesize=50&skippages=2", exchanges.get(0).getRequestURI().toString());
        assertHeaders(exchanges.get(0));
    }

    @Test
    void retrievesOneProduct() {
        responseBody = "{\"productNumber\":\"P-2\",\"name\":\"Example\"}";

        EconomicProductResponse response = client().getProduct("P-2");

        assertEquals("P-2", response.getProductNumber());
        assertEquals("/products/P-2", exchanges.get(0).getRequestURI().toString());
    }

    @Test
    void createsProductWithStableIdempotencyKey() {
        responseBody = "{\"productNumber\":\"P-3\",\"name\":\"Example\"}";
        EconomicProductRequest request = new EconomicProductRequest();
        request.setProductNumber("P-3");
        request.setName("Example");
        request.setProductGroup(new EconomicProductRequest.ProductGroup(1));

        EconomicProductResponse response = client().createProduct(request, "idem-key");

        assertEquals("P-3", response.getProductNumber());
        assertEquals("idem-key", exchanges.get(0).getRequestHeaders().getFirst("Idempotency-Key"));
        assertEquals("POST", exchanges.get(0).getRequestMethod());
        assertTrue(requestBodies.get(0).contains("\"productNumber\":\"P-3\""));
        assertHeaders(exchanges.get(0));
    }

    @Test
    void updatesProduct() {
        responseBody = "{\"productNumber\":\"P-4\",\"name\":\"Updated\"}";
        EconomicProductRequest request = new EconomicProductRequest();
        request.setProductNumber("P-4");
        request.setName("Updated");
        request.setSalesPrice(new BigDecimal("100.00"));

        EconomicProductResponse response = client().updateProduct("P-4", request);

        assertEquals("P-4", response.getProductNumber());
        assertEquals("PUT", exchanges.get(0).getRequestMethod());
        assertEquals("/products/P-4", exchanges.get(0).getRequestURI().toString());
        assertTrue(requestBodies.get(0).contains("\"salesPrice\":100.00"));
    }

    @Test
    void encodesProductNumbersInPaths() {
        responseBody = "{\"productNumber\":\"P 5\",\"name\":\"Example\"}";

        client().getProduct("P 5");

        assertEquals("/products/P%205", exchanges.get(0).getRequestURI().toString());
    }

    @Test
    void convertsNotFoundValidationAuthRateLimitAndUpstreamErrorsSafely() {
        assertStatus(400, 400);
        assertStatus(401, 401);
        assertStatus(403, 403);
        assertStatus(404, 404);
        assertStatus(409, 409);
        assertStatus(429, 429);
        assertStatus(500, 502);
    }

    @Test
    void doesNotExposeCredentialsInErrors() {
        responseStatus = 401;
        responseBody = "{\"message\":\"bad secret-token\",\"logId\":\"abc\"}";

        ApiException exception = assertThrows(ApiException.class, () -> client().listProducts(20, 0));

        assertFalse(exception.getMessage().contains("app-secret"));
        assertFalse(exception.getMessage().contains("agreement-token"));
    }

    private void assertStatus(int upstreamStatus, int applicationStatus) {
        responseStatus = upstreamStatus;
        responseBody = "{\"message\":\"upstream details\",\"logId\":\"log-1\"}";

        ApiException exception = assertThrows(ApiException.class, () -> client().listProducts(20, 0));

        assertEquals(applicationStatus, exception.getCode());
    }

    private EconomicProductClient client() {
        return new EconomicProductClient(HttpClient.newHttpClient(), "http://localhost:" + server.getAddress().getPort(), "app-secret", "agreement-token");
    }

    private void assertHeaders(HttpExchange exchange) {
        assertEquals("app-secret", exchange.getRequestHeaders().getFirst("X-AppSecretToken"));
        assertEquals("agreement-token", exchange.getRequestHeaders().getFirst("X-AgreementGrantToken"));
        assertEquals("application/json", exchange.getRequestHeaders().getFirst("Accept"));
    }

}

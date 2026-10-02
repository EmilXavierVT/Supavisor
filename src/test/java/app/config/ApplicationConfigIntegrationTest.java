package app.config;

import app.dao.UserDAO;
import app.entities.User;
import app.services.routeSecurity.RoutePackage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class ApplicationConfigIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_javalin_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;
    private static ApplicationConfig applicationConfig;
    private static Javalin app;
    private static HttpClient httpClient;
    private static ObjectMapper objectMapper;
    private static UserDAO userDAO;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
        RoutePackage routes = new RoutePackage(emf);
        applicationConfig = new ApplicationConfig(emf);
        app = applicationConfig
                .cors()
                .apiExceptions()
                .exceptions()
                .notFound()
                .security()
                .route(routes.getRoutes())
                .start(0);
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();
        userDAO = new UserDAO(emf);
    }

    @AfterAll
    static void tearDown() {
        if (applicationConfig != null) {
            applicationConfig.stopServer();
        }
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void javalinServerStartsAndExposesHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/system/health")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"ok\"}", response.body());
    }

    @Test
    void authRegisterRoutePersistsUserThroughDatabase() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"server-test@example.com\",\"password\":\"secret-password\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"msg\":\"register success\""));
        assertTrue(response.body().contains("\"id\":"));
    }

    @Test
    void authTokenValidationAcceptsValidBearerToken() throws Exception {
        String email = "token-validation-test@example.com";
        String password = "secret-password";
        register(email, password);
        String token = loginAndGetToken(email, password);

        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/token-validation"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("{\"msg\":\"Token is valid\"}", response.body());
    }

    @Test
    void missingAuthenticationReturnsConsistentJsonError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/protected")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(401, response.statusCode());
        assertEquals(401, body.get("status").asInt());
        assertEquals("Authorization header is missing", body.get("msg").asText());
    }

    @Test
    void malformedJsonReturnsConsistentValidationError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(400, response.statusCode());
        assertEquals(400, body.get("status").asInt());
        assertEquals("Request body contains invalid JSON", body.get("msg").asText());
    }

    @Test
    void notFoundReturnsConsistentJsonError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/does-not-exist")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(404, response.statusCode());
        assertEquals(404, body.get("status").asInt());
        assertEquals("Not found", body.get("msg").asText());
    }

    private static void register(String email, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    private static String loginAndGetToken(String email, String password) throws Exception {
        HttpResponse<String> response = login(email, password);

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        return body.get("token").asText();
    }

    private static HttpResponse<String> login(String email, String password) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private static URI uri(String path) {
        return URI.create("http://localhost:" + app.port() + path);
    }
}

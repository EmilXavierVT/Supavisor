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
    void employeeCanLoginAndAccessEmployeeArea() throws Exception {
        String email = uniqueEmail();
        String password = "secret-password";
        register(email, password);

        HttpResponse<String> login = login(email, password);
        JsonNode loginBody = objectMapper.readTree(login.body());
        String token = loginBody.get("token").asText();

        assertEquals(200, login.statusCode());
        assertEquals(email, loginBody.get("username").asText());
        assertEquals("USER", loginBody.get("role").asText());
        assertTrue(loginBody.get("activity").asBoolean());

        HttpRequest protectedRequest = HttpRequest.newBuilder(uri("/api/auth/protected"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> protectedResponse = httpClient.send(protectedRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, protectedResponse.statusCode());
        assertTrue(protectedResponse.body().contains("HELLO FROM THE RESTRICTED AREA"));
    }

    @Test
    void invalidEmployeeCredentialsAreRejected() throws Exception {
        String email = uniqueEmail();
        register(email, "secret-password");

        HttpResponse<String> login = login(email, "wrong-password");

        assertEquals(401, login.statusCode());
        assertTrue(login.body().contains("Invalid email or password"));
    }

    @Test
    void inactiveEmployeeCannotLogin() throws Exception {
        String email = uniqueEmail();
        userDAO.create(new User(null, email, "secret-password", null, null, false, Set.of("USER")));

        HttpResponse<String> login = login(email, "secret-password");

        assertEquals(401, login.statusCode());
        assertTrue(login.body().contains("User is inactive"));
    }

    @Test
    void employeeLoginDoesNotGrantAdministrativeCapabilities() throws Exception {
        String email = uniqueEmail();
        String password = "secret-password";
        register(email, password);
        String token = loginAndGetToken(email, password);

        HttpRequest adminRequest = HttpRequest.newBuilder(uri("/api/user/create"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"secret-password\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(adminRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
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

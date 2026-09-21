package app.services.routeSecurity.routes;

import app.config.ApplicationConfig;
import app.config.TestEntityManagerFactory;
import app.dao.UserDAO;
import app.entities.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Talks to a real Javalin server over HTTP, so it covers routing, token security and role checks. */
@Testcontainers
class AssignmentApiIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_assignment_test")
            .withUsername("test")
            .withPassword("test");

    private static final String PASSWORD = "secret-password";
    private static final long TENANT = 501L;
    private static final long OTHER_TENANT = 502L;

    private static EntityManagerFactory emf;
    private static ApplicationConfig applicationConfig;
    private static Javalin app;
    private static HttpClient httpClient;
    private static ObjectMapper objectMapper;
    private static String adminToken;
    private static String userToken;
    private static String otherTenantAdminToken;

    @BeforeAll
    static void setUp() throws Exception {
        emf = TestEntityManagerFactory.create(POSTGRES);
        Routes routes = new Routes(emf);
        applicationConfig = new ApplicationConfig(emf);
        app = applicationConfig
                .cors()
                .apiExceptions()
                .exceptions()
                .notFound()
                .security()
                .route(routes.getRoutes())
                .route(routes.getRouteResource("auth"))
                .start(0);
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();

        adminToken = tokenFor("ADMIN", TENANT);
        userToken = tokenFor("USER", TENANT);
        otherTenantAdminToken = tokenFor("ADMIN", OTHER_TENANT);
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

    // ---- CRUD as an administrator

    @Test
    void adminCanCreateReadUpdateAndDeleteAnAssignment() throws Exception {
        HttpResponse<String> created = send("POST", "/api/assignment", adminToken, "{\"name\":\"Cleaning\"}");
        assertEquals(201, created.statusCode());
        JsonNode body = json(created);
        long id = body.get("id").asLong();
        assertEquals("Cleaning", body.get("name").asText());
        assertTrue(body.get("isActive").asBoolean());
        assertEquals(TENANT, body.get("tenantId").asLong());
        assertEquals("Cleaning", dbName(id));

        HttpResponse<String> read = send("GET", "/api/assignment/" + id, adminToken, null);
        assertEquals(200, read.statusCode());
        assertEquals("Cleaning", json(read).get("name").asText());

        HttpResponse<String> updated = send("PUT", "/api/assignment/" + id, adminToken,
                "{\"name\":\"Deep cleaning\",\"isActive\":true}");
        assertEquals(200, updated.statusCode());
        assertEquals("Deep cleaning", json(updated).get("name").asText());
        assertEquals("Deep cleaning", dbName(id));

        assertEquals(204, send("DELETE", "/api/assignment/" + id, adminToken, null).statusCode());
        assertEquals(0, dbCount(id));
        assertEquals(404, send("GET", "/api/assignment/" + id, adminToken, null).statusCode());
    }

    @Test
    void adminSeesEveryAssignmentAndCanFilterToActiveOnes() throws Exception {
        String active = unique("Active");
        String inactive = unique("Inactive");
        create(adminToken, active);
        long inactiveId = create(adminToken, inactive);
        assertEquals(200, send("PATCH", "/api/assignment/" + inactiveId + "/deactivate", adminToken, null).statusCode());

        String all = send("GET", "/api/assignment/all", adminToken, null).body();
        assertTrue(all.contains(active) && all.contains(inactive));

        String activeOnly = send("GET", "/api/assignment/all?activeOnly=true", adminToken, null).body();
        assertTrue(activeOnly.contains(active));
        assertFalse(activeOnly.contains(inactive));
    }

    @Test
    void deactivateFlipsTheFlagButKeepsTheRow() throws Exception {
        long id = create(adminToken, unique("Kitchen"));

        HttpResponse<String> response = send("PATCH", "/api/assignment/" + id + "/deactivate", adminToken, null);

        assertEquals(200, response.statusCode());
        assertFalse(json(response).get("isActive").asBoolean());
        assertEquals(1, dbCount(id));
    }

    @Test
    void invalidAndDuplicateNamesAreRejected() throws Exception {
        String name = unique("Cleaning");
        create(adminToken, name);

        assertEquals(400, send("POST", "/api/assignment", adminToken, "{}").statusCode());
        assertEquals(400, send("POST", "/api/assignment", adminToken, "{\"name\":\"   \"}").statusCode());
        assertEquals(400, send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + "a".repeat(101) + "\"}").statusCode());
        HttpResponse<String> duplicate = send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + name.toUpperCase() + "\"}");
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("already exists"));
    }

    // ---- details: address, estimated time, cost, employee

    @Test
    void adminCanFillInAndChangeAddressEstimatedTimeCostAndEmployee() throws Exception {
        long first = employeeId(TENANT);
        long second = employeeId(TENANT);
        String name = unique("Cleaning at Main Street");

        HttpResponse<String> created = send("POST", "/api/assignment", adminToken, "{\"name\":\"" + name
                + "\",\"address\":\"Main Street 1\",\"estimatedMinutes\":90,\"cost\":1250.5,\"assignedEmployeeId\":"
                + first + "}");
        assertEquals(201, created.statusCode());
        JsonNode body = json(created);
        long id = body.get("id").asLong();
        assertEquals("Main Street 1", body.get("address").asText());
        assertEquals(90, body.get("estimatedMinutes").asInt());
        assertEquals(0, new BigDecimal("1250.50").compareTo(body.get("cost").decimalValue()));
        assertEquals(first, body.get("assignedEmployeeId").asLong());

        HttpResponse<String> read = send("GET", "/api/assignment/" + id, adminToken, null);
        assertEquals("Main Street 1", json(read).get("address").asText());
        assertEquals(first, json(read).get("assignedEmployeeId").asLong());

        HttpResponse<String> updated = send("PUT", "/api/assignment/" + id, adminToken, "{\"name\":\"" + name
                + "\",\"address\":\"Second Street 2\",\"estimatedMinutes\":120,\"cost\":99,\"assignedEmployeeId\":"
                + second + "}");
        assertEquals(200, updated.statusCode());
        assertEquals("Second Street 2", json(updated).get("address").asText());
        assertEquals(second, json(updated).get("assignedEmployeeId").asLong());
        assertEquals(second, dbEmployee(id));

        HttpResponse<String> cleared = send("PUT", "/api/assignment/" + id, adminToken, "{\"name\":\"" + name + "\"}");
        assertEquals(200, cleared.statusCode());
        assertTrue(json(cleared).get("address").isNull());
        assertTrue(json(cleared).get("assignedEmployeeId").isNull());
    }

    @Test
    void invalidDetailsAndForeignEmployeesAreRejected() throws Exception {
        long employeeOfAnotherTenant = employeeId(OTHER_TENANT);

        assertEquals(400, send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + unique("A") + "\",\"cost\":-1}").statusCode());
        assertEquals(400, send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + unique("A") + "\",\"estimatedMinutes\":0}").statusCode());
        HttpResponse<String> foreign = send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + unique("A") + "\",\"assignedEmployeeId\":" + employeeOfAnotherTenant + "}");
        assertEquals(400, foreign.statusCode());
        assertTrue(foreign.body().contains("Employee not found"));
    }

    // ---- activate

    @Test
    void adminCanActivateADeactivatedAssignmentButAUserCannot() throws Exception {
        long id = create(adminToken, unique("Seasonal"));
        send("PATCH", "/api/assignment/" + id + "/deactivate", adminToken, null);

        assertEquals(403, send("PATCH", "/api/assignment/" + id + "/activate", userToken, null).statusCode());
        assertFalse(dbActive(id));

        HttpResponse<String> activated = send("PATCH", "/api/assignment/" + id + "/activate", adminToken, null);
        assertEquals(200, activated.statusCode());
        assertTrue(json(activated).get("isActive").asBoolean());
        assertTrue(dbActive(id));
        assertEquals(404, send("PATCH", "/api/assignment/" + id + "/activate", otherTenantAdminToken, null).statusCode());
    }

    // ---- role permissions

    @Test
    void requestsWithoutAValidTokenAreRejected() throws Exception {
        assertEquals(401, send("GET", "/api/assignment/all", null, null).statusCode());
        assertEquals(401, send("POST", "/api/assignment", null, "{\"name\":\"X\"}").statusCode());
        assertEquals(401, send("GET", "/api/assignment/all", "not-a-real-token", null).statusCode());
    }

    @Test
    void aNonAdministratorCannotManageAssignments() throws Exception {
        long id = create(adminToken, unique("Protected"));

        assertEquals(403, send("POST", "/api/assignment", userToken, "{\"name\":\"Sneaky\"}").statusCode());
        assertEquals(403, send("PUT", "/api/assignment/" + id, userToken, "{\"name\":\"Sneaky\"}").statusCode());
        assertEquals(403, send("PATCH", "/api/assignment/" + id + "/deactivate", userToken, null).statusCode());
        assertEquals(403, send("DELETE", "/api/assignment/" + id, userToken, null).statusCode());

        // nothing changed
        assertEquals(1, dbCount(id));
        assertTrue(dbActive(id));
    }

    @Test
    void aNonAdministratorOnlySeesActiveAssignments() throws Exception {
        String active = unique("Visible");
        String inactive = unique("Hidden");
        create(adminToken, active);
        long inactiveId = create(adminToken, inactive);
        send("PATCH", "/api/assignment/" + inactiveId + "/deactivate", adminToken, null);

        HttpResponse<String> list = send("GET", "/api/assignment/all", userToken, null);

        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains(active));
        assertFalse(list.body().contains(inactive));
        assertEquals(404, send("GET", "/api/assignment/" + inactiveId, userToken, null).statusCode());
    }

    // ---- tenant boundary

    @Test
    void anAdministratorOfAnotherTenantCannotSeeOrTouchTheAssignment() throws Exception {
        String name = unique("Private");
        long id = create(adminToken, name);

        assertFalse(send("GET", "/api/assignment/all", otherTenantAdminToken, null).body().contains(name));
        assertEquals(404, send("GET", "/api/assignment/" + id, otherTenantAdminToken, null).statusCode());
        assertEquals(404, send("PUT", "/api/assignment/" + id, otherTenantAdminToken, "{\"name\":\"Hacked\"}").statusCode());
        assertEquals(404, send("PATCH", "/api/assignment/" + id + "/deactivate", otherTenantAdminToken, null).statusCode());
        assertEquals(404, send("DELETE", "/api/assignment/" + id, otherTenantAdminToken, null).statusCode());
        assertEquals(name, dbName(id));
        assertTrue(dbActive(id));
    }

    @Test
    void theTenantInTheRequestBodyIsIgnored() throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + unique("Sticky") + "\",\"tenantId\":" + OTHER_TENANT + "}");

        assertEquals(201, response.statusCode());
        assertEquals(TENANT, json(response).get("tenantId").asLong());
    }

    // ---- helpers

    private static long employeeId(long tenantId) {
        return new UserDAO(emf).create(new User(null, UUID.randomUUID() + "@example.com", PASSWORD, null,
                tenantId, true, Set.of("USER"))).getId();
    }

    private static Long dbEmployee(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Number value = (Number) em.createNativeQuery("SELECT assigned_employee_id FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
            return value == null ? null : value.longValue();
        }
    }

    private static long create(String token, String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", token, "{\"name\":\"" + name + "\"}");
        assertEquals(201, response.statusCode());
        return json(response).get("id").asLong();
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String tokenFor(String role, long tenantId) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        new UserDAO(emf).create(new User(null, email, PASSWORD, null, tenantId, true, Set.of(role)));

        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, login.statusCode());
        return json(login).get("token").asText();
    }

    private static HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        builder.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private static String dbName(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return (String) em.createNativeQuery("SELECT name FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        }
    }

    private static boolean dbActive(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return (Boolean) em.createNativeQuery("SELECT is_active FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        }
    }

    private static int dbCount(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult()).intValue();
        }
    }
}

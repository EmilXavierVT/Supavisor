package app.services.routeSecurity.routes;

import app.config.ApplicationConfig;
import app.config.TestEntityManagerFactory;
import app.dao.UserDAO;
import app.entities.User;
import app.services.routeSecurity.RoutePackage;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private record TokenUser(String token, long userId) {}

    @BeforeAll
    static void setUp() throws Exception {
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

    // ---- details: address, time window, estimated time, cost, employee

    @Test
    void adminCanFillInAndChangeAddressTimeWindowEstimatedTimeCostAndEmployee() throws Exception {
        long first = employeeId(TENANT);
        long second = employeeId(TENANT);
        String name = unique("Cleaning at Main Street");

        HttpResponse<String> created = send("POST", "/api/assignment", adminToken, "{\"name\":\"" + name
                + "\",\"address\":\"Main Street 1\",\"startTime\":\"2026-10-05T14:30:00\",\"estimatedEndTime\":\"2026-10-05T16:00:00\",\"estimatedMinutes\":90,\"cost\":1250.5,\"assignedEmployeeId\":"
                + first + "}");
        assertEquals(201, created.statusCode());
        JsonNode body = json(created);
        long id = body.get("id").asLong();
        assertEquals("Main Street 1", body.get("address").asText());
        assertEquals("2026-10-05T14:30:00", body.get("startTime").asText());
        assertEquals("2026-10-05T16:00:00", body.get("estimatedEndTime").asText());
        assertEquals(90, body.get("estimatedMinutes").asInt());
        assertEquals(0, new BigDecimal("1250.50").compareTo(body.get("cost").decimalValue()));
        assertEquals(first, body.get("assignedEmployeeId").asLong());

        HttpResponse<String> read = send("GET", "/api/assignment/" + id, adminToken, null);
        assertEquals("Main Street 1", json(read).get("address").asText());
        assertEquals("2026-10-05T14:30:00", json(read).get("startTime").asText());
        assertEquals("2026-10-05T16:00:00", json(read).get("estimatedEndTime").asText());
        assertEquals(first, json(read).get("assignedEmployeeId").asLong());

        HttpResponse<String> updated = send("PUT", "/api/assignment/" + id, adminToken, "{\"name\":\"" + name
                + "\",\"address\":\"Second Street 2\",\"startTime\":\"2026-10-06T09:15:00\",\"estimatedEndTime\":\"2026-10-06T11:15:00\",\"estimatedMinutes\":120,\"cost\":99,\"assignedEmployeeId\":"
                + second + "}");
        assertEquals(200, updated.statusCode());
        assertEquals("Second Street 2", json(updated).get("address").asText());
        assertEquals("2026-10-06T09:15:00", json(updated).get("startTime").asText());
        assertEquals("2026-10-06T11:15:00", json(updated).get("estimatedEndTime").asText());
        assertEquals(second, json(updated).get("assignedEmployeeId").asLong());
        assertEquals(LocalDateTime.of(2026, 10, 6, 9, 15), dbDateTime(id, "start_time"));
        assertEquals(LocalDateTime.of(2026, 10, 6, 11, 15), dbDateTime(id, "estimated_end_time"));
        assertEquals(second, dbEmployee(id));

        HttpResponse<String> cleared = send("PUT", "/api/assignment/" + id, adminToken, "{\"name\":\"" + name + "\"}");
        assertEquals(200, cleared.statusCode());
        assertTrue(json(cleared).get("address").isNull());
        assertTrue(json(cleared).get("startTime").isNull());
        assertTrue(json(cleared).get("estimatedEndTime").isNull());
        assertTrue(json(cleared).get("assignedEmployeeId").isNull());
    }

    @Test
    void estimatedEndTimeMustBeLaterThanStartTime() throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + unique("Bad time")
                        + "\",\"startTime\":\"2026-10-05T14:30:00\",\"estimatedEndTime\":\"2026-10-05T14:30:00\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Estimated end time must be later than start time"));
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

    // ---- assignment states and attendance timestamps

    @Test
    void assignmentsExposeDefaultStateAndInitialStateHistory() throws Exception {
        HttpResponse<String> created = send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + unique("Stateful") + "\"}");
        assertEquals(201, created.statusCode());
        JsonNode body = json(created);
        long id = body.get("id").asLong();

        assertEquals("PLANNED", body.get("state").asText());
        assertTrue(body.get("checkInAt").isNull());
        assertTrue(body.get("checkOutAt").isNull());

        HttpResponse<String> history = send("GET", "/api/assignment/" + id + "/state-history", adminToken, null);
        assertEquals(200, history.statusCode());
        JsonNode entries = json(history);
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).get("fromState").isNull());
        assertEquals("PLANNED", entries.get(0).get("toState").asText());
        assertEquals("system", entries.get(0).get("source").asText());
        assertNotNull(entries.get(0).get("changedAt").asText());
    }

    @Test
    void adminCanChangeAssignmentStateAndInvalidTransitionsAreRejected() throws Exception {
        long id = create(adminToken, unique("Transition"));

        HttpResponse<String> acknowledged = send("PATCH", "/api/assignment/" + id + "/state", adminToken,
                "{\"state\":\"ACKNOWLEDGED\"}");
        assertEquals(200, acknowledged.statusCode());
        assertEquals("ACKNOWLEDGED", json(acknowledged).get("state").asText());

        HttpResponse<String> invalid = send("PATCH", "/api/assignment/" + id + "/state", adminToken,
                "{\"state\":\"COMPLETED\"}");
        assertEquals(409, invalid.statusCode());
        assertTrue(invalid.body().contains("cannot transition"));

        HttpResponse<String> history = send("GET", "/api/assignment/" + id + "/state-history", adminToken, null);
        assertEquals(200, history.statusCode());
        JsonNode entries = json(history);
        assertEquals(2, entries.size());
        assertEquals("PLANNED", entries.get(0).get("fromState").asText());
        assertEquals("ACKNOWLEDGED", entries.get(0).get("toState").asText());
        assertEquals("ACKNOWLEDGED", json(send("GET", "/api/assignment/" + id, adminToken, null))
                .get("state").asText());
    }

    @Test
    void checkInAndCheckOutUseServerTimestampsAndReturnStoredValues() throws Exception {
        TokenUser employee = tokenUserFor("USER", TENANT);
        long id = assignedAssignment(employee.userId(), unique("Attendance"));

        Instant beforeCheckIn = Instant.now().minusSeconds(1);
        HttpResponse<String> checkedIn = send("PATCH", "/api/assignment/" + id + "/check-in", employee.token(),
                "{\"checkInAt\":\"2001-01-01T00:00:00Z\"}");
        Instant afterCheckIn = Instant.now().plusSeconds(1);
        assertEquals(200, checkedIn.statusCode());
        JsonNode checkInBody = json(checkedIn);
        Instant checkInAt = Instant.parse(checkInBody.get("checkInAt").asText());
        assertTrue(!checkInAt.isBefore(beforeCheckIn) && !checkInAt.isAfter(afterCheckIn));
        assertEquals("IN_PROGRESS", checkInBody.get("state").asText());

        JsonNode readAfterCheckIn = json(send("GET", "/api/assignment/" + id, adminToken, null));
        assertEquals(checkInAt, Instant.parse(readAfterCheckIn.get("checkInAt").asText()));
        assertEquals("IN_PROGRESS", readAfterCheckIn.get("state").asText());

        HttpResponse<String> duplicate = send("PATCH", "/api/assignment/" + id + "/check-in", employee.token(), null);
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("already checked in"));
        assertEquals(checkInAt, Instant.parse(json(send("GET", "/api/assignment/" + id, adminToken, null))
                .get("checkInAt").asText()));

        Instant beforeCheckOut = Instant.now().minusSeconds(1);
        HttpResponse<String> checkedOut = send("PATCH", "/api/assignment/" + id + "/check-out", employee.token(),
                "{\"checkOutAt\":\"2001-01-01T00:00:00Z\"}");
        Instant afterCheckOut = Instant.now().plusSeconds(1);
        assertEquals(200, checkedOut.statusCode());
        JsonNode checkOutBody = json(checkedOut);
        Instant checkOutAt = Instant.parse(checkOutBody.get("checkOutAt").asText());
        assertTrue(!checkOutAt.isBefore(beforeCheckOut) && !checkOutAt.isAfter(afterCheckOut));
        assertEquals("COMPLETED", checkOutBody.get("state").asText());

        JsonNode readAfterCheckOut = json(send("GET", "/api/assignment/" + id, adminToken, null));
        assertEquals(checkInAt, Instant.parse(readAfterCheckOut.get("checkInAt").asText()));
        assertEquals(checkOutAt, Instant.parse(readAfterCheckOut.get("checkOutAt").asText()));
        assertEquals("COMPLETED", readAfterCheckOut.get("state").asText());

        HttpResponse<String> duplicateCheckout = send("PATCH", "/api/assignment/" + id + "/check-out", employee.token(), null);
        assertEquals(409, duplicateCheckout.statusCode());
        assertTrue(duplicateCheckout.body().contains("already checked out"));
        assertEquals(checkOutAt, Instant.parse(json(send("GET", "/api/assignment/" + id, adminToken, null))
                .get("checkOutAt").asText()));
    }

    @Test
    void checkOutRequiresCheckInAndAttendanceTimestampsCannotBeSetThroughUpdate() throws Exception {
        TokenUser employee = tokenUserFor("USER", TENANT);
        long id = assignedAssignment(employee.userId(), unique("Protected attendance"));

        HttpResponse<String> noActiveCheckIn = send("PATCH", "/api/assignment/" + id + "/check-out", employee.token(), null);
        assertEquals(409, noActiveCheckIn.statusCode());
        assertTrue(noActiveCheckIn.body().contains("no active check-in"));

        HttpResponse<String> updated = send("PUT", "/api/assignment/" + id, adminToken,
                "{\"name\":\"Protected attendance renamed\",\"checkInAt\":\"2001-01-01T00:00:00Z\",\"checkOutAt\":\"2001-01-01T01:00:00Z\"}");

        assertEquals(200, updated.statusCode());
        assertTrue(json(updated).get("checkInAt").isNull());
        assertTrue(json(updated).get("checkOutAt").isNull());
    }

    @Test
    void onlyAssignedEmployeeCanCheckOut() throws Exception {
        TokenUser employee = tokenUserFor("USER", TENANT);
        TokenUser otherEmployee = tokenUserFor("USER", TENANT);
        long assigned = assignedAssignment(employee.userId(), unique("Checkout owner"));
        assertEquals(200, send("PATCH", "/api/assignment/" + assigned + "/check-in", employee.token(), null).statusCode());

        assertEquals(404, send("PATCH", "/api/assignment/" + assigned + "/check-out", otherEmployee.token(), null).statusCode());
        assertTrue(json(send("GET", "/api/assignment/" + assigned, adminToken, null)).get("checkOutAt").isNull());
        assertEquals(200, send("PATCH", "/api/assignment/" + assigned + "/check-out", employee.token(), null).statusCode());
    }

    @Test
    void onlyAssignedEmployeeCanCheckIn() throws Exception {
        TokenUser employee = tokenUserFor("USER", TENANT);
        TokenUser otherEmployee = tokenUserFor("USER", TENANT);
        long assigned = assignedAssignment(employee.userId(), unique("Assigned attendance"));
        long unassigned = create(adminToken, unique("Unassigned attendance"));

        assertEquals(200, send("PATCH", "/api/assignment/" + assigned + "/check-in", employee.token(), null).statusCode());
        assertEquals(404, send("PATCH", "/api/assignment/" + assigned + "/check-in", otherEmployee.token(), null).statusCode());
        assertEquals(404, send("PATCH", "/api/assignment/" + unassigned + "/check-in", employee.token(), null).statusCode());
    }

    @Test
    void checkInRejectsAssignmentsInTerminalStates() throws Exception {
        TokenUser employee = tokenUserFor("USER", TENANT);
        String[] terminalStates = {"CANCELLED", "DECLINED", "COMPLETED"};

        for (String state : terminalStates) {
            HttpResponse<String> created = send("POST", "/api/assignment", adminToken,
                    "{\"name\":\"" + unique(state) + "\",\"state\":\"" + state
                            + "\",\"assignedEmployeeId\":" + employee.userId() + "}");
            assertEquals(201, created.statusCode());
            long id = json(created).get("id").asLong();

            HttpResponse<String> rejected = send("PATCH", "/api/assignment/" + id + "/check-in", employee.token(), null);

            assertEquals(409, rejected.statusCode());
            assertTrue(rejected.body().contains("not eligible"));
            JsonNode unchanged = json(send("GET", "/api/assignment/" + id, adminToken, null));
            assertEquals(state, unchanged.get("state").asText());
            assertTrue(unchanged.get("checkInAt").isNull());
        }
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
        assertEquals(403, send("PATCH", "/api/assignment/" + id + "/state", userToken, "{\"state\":\"CANCELLED\"}").statusCode());
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

    private static LocalDateTime dbDateTime(long id, String column) {
        try (EntityManager em = emf.createEntityManager()) {
            Object value = em.createNativeQuery("SELECT " + column + " FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
            if (value instanceof java.sql.Timestamp timestamp) {
                return timestamp.toLocalDateTime();
            }
            return (LocalDateTime) value;
        }
    }

    private static long create(String token, String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", token, "{\"name\":\"" + name + "\"}");
        assertEquals(201, response.statusCode());
        return json(response).get("id").asLong();
    }

    private static long assignedAssignment(long employeeId, String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", adminToken,
                "{\"name\":\"" + name + "\",\"assignedEmployeeId\":" + employeeId + "}");
        assertEquals(201, response.statusCode());
        return json(response).get("id").asLong();
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String tokenFor(String role, long tenantId) throws Exception {
        return tokenUserFor(role, tenantId).token();
    }

    private static TokenUser tokenUserFor(String role, long tenantId) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        User user = new UserDAO(emf).create(new User(null, email, PASSWORD, null, tenantId, true, Set.of(role)));

        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, login.statusCode());
        return new TokenUser(json(login).get("token").asText(), user.getId());
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

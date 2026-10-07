package app.services.routeSecurity.routes;

import app.config.ApplicationConfig;
import app.config.TestEntityManagerFactory;
import app.dao.ProductDAO;
import app.dao.UserDAO;
import app.entities.Product;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class AssignmentResponsibleApiIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_responsible_test")
            .withUsername("test")
            .withPassword("test");

    private static final String PASSWORD = "secret-password";
    private static final long TENANT = 601L;
    private static final long OTHER_TENANT = 602L;

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

    @Test
    void anAdministratorCanSelectAnActiveEmployeeAsResponsible() throws Exception {
        long id = create(unique("Cleaning"));
        long employee = employeeId(TENANT, "Anna Andersen", true);

        HttpResponse<String> response = setResponsible(adminToken, id, employee);

        assertEquals(200, response.statusCode());
        assertEquals(id, json(response).get("id").asLong());
        assertEquals(employee, json(response).get("assignedEmployeeId").asLong());
        assertEquals("Anna Andersen", json(response).get("assignedEmployeeName").asText());
        assertEquals(employee, dbEmployee(id));
    }

    @Test
    void anAssignmentHasOnlyOneResponsiblePerson() throws Exception {
        long id = create(unique("Cleaning"));
        long first = employeeId(TENANT, "First Person", true);
        long second = employeeId(TENANT, "Second Person", true);

        assertEquals(200, setResponsible(adminToken, id, first).statusCode());
        assertEquals(200, setResponsible(adminToken, id, second).statusCode());
        HttpResponse<String> again = setResponsible(adminToken, id, second);

        assertEquals(200, again.statusCode());
        assertTrue(json(again).get("assignedEmployeeId").isIntegralNumber());
        assertTrue(json(again).get("assignedEmployeeName").isTextual());
        assertEquals(second, json(again).get("assignedEmployeeId").asLong());
        assertEquals(second, dbEmployee(id));
        assertEquals(1, dbCount(id));
    }

    @Test
    void theResponsiblePersonIsVisibleOnTheAssignment() throws Exception {
        String name = unique("Visible");
        long id = create(name);
        long withoutResponsible = create(unique("Nobody"));
        long employee = employeeId(TENANT, "Bente Berg", true);
        assertEquals(200, setResponsible(adminToken, id, employee).statusCode());

        JsonNode one = json(send("GET", "/api/assignment/" + id, adminToken, null));
        assertEquals(employee, one.get("assignedEmployeeId").asLong());
        assertEquals("Bente Berg", one.get("assignedEmployeeName").asText());

        JsonNode listed = inList(send("GET", "/api/assignment/all", adminToken, null), id);
        assertEquals(employee, listed.get("assignedEmployeeId").asLong());
        assertEquals("Bente Berg", listed.get("assignedEmployeeName").asText());

        JsonNode empty = inList(send("GET", "/api/assignment/all", adminToken, null), withoutResponsible);
        assertTrue(empty.get("assignedEmployeeId").isNull());
        assertTrue(empty.get("assignedEmployeeName").isNull());
    }

    @Test
    void reassigningReplacesTheCurrentResponsiblePerson() throws Exception {
        long id = create(unique("Cleaning"));
        long first = employeeId(TENANT, "Old Responsible", true);
        long second = employeeId(TENANT, "New Responsible", true);
        assertEquals(200, setResponsible(adminToken, id, first).statusCode());

        HttpResponse<String> reassigned = setResponsible(adminToken, id, second);

        assertEquals(200, reassigned.statusCode());
        assertEquals(second, json(reassigned).get("assignedEmployeeId").asLong());
        assertEquals("New Responsible", json(reassigned).get("assignedEmployeeName").asText());
        assertEquals(second, dbEmployee(id));
        HttpResponse<String> read = send("GET", "/api/assignment/" + id, adminToken, null);
        assertEquals(second, json(read).get("assignedEmployeeId").asLong());
        assertFalse(read.body().contains("Old Responsible"));
    }

    @Test
    void anInactiveOrUnknownEmployeeIsRejected() throws Exception {
        long id = create(unique("Cleaning"));
        long current = employeeId(TENANT, "Current Person", true);
        long inactive = employeeId(TENANT, "Inactive Person", false);
        assertEquals(200, setResponsible(adminToken, id, current).statusCode());

        HttpResponse<String> deactivated = setResponsible(adminToken, id, inactive);
        assertEquals(400, deactivated.statusCode());
        assertTrue(deactivated.body().contains("deactivated"));

        HttpResponse<String> nonexistent = setResponsible(adminToken, id, 999_999_999L);
        assertEquals(400, nonexistent.statusCode());
        assertTrue(nonexistent.body().contains("Employee not found"));

        assertEquals(400, send("PUT", "/api/assignment/" + id + "/responsible", adminToken, "{}").statusCode());
        assertEquals(400, send("PUT", "/api/assignment/" + id + "/responsible", adminToken,
                "{\"assignedEmployeeId\":null}").statusCode());

        assertEquals(current, dbEmployee(id));
    }

    @Test
    void settingTheResponsiblePersonDoesNotChangeAddressCostTimesOrProducts() throws Exception {
        long product = productId(TENANT);
        long otherProduct = productId(TENANT);
        long id = createWithDetails(unique("Detailed"), product, otherProduct);
        Object[] before = dbDetails(id);
        long employee = employeeId(TENANT, "Carl Carlsen", true);

        HttpResponse<String> response = setResponsible(adminToken, id, employee);

        assertEquals(200, response.statusCode());
        assertDetailsUntouched(json(response), product, otherProduct);
        assertEquals(employee, json(response).get("assignedEmployeeId").asLong());
        assertDetailsUntouched(json(send("GET", "/api/assignment/" + id, adminToken, null)), product, otherProduct);
        assertDbDetailsEqual(before, dbDetails(id));
        assertEquals(List.of(product, otherProduct), dbProducts(id));
        assertEquals(employee, dbEmployee(id));
    }

    @Test
    void otherFieldsInTheBodyOfTheResponsibleEndpointAreIgnored() throws Exception {
        long product = productId(TENANT);
        long otherProduct = productId(TENANT);
        String name = unique("Detailed");
        long id = createWithDetails(name, product, otherProduct);
        Object[] before = dbDetails(id);
        long employee = employeeId(TENANT, "Dorte Dam", true);

        HttpResponse<String> response = send("PUT", "/api/assignment/" + id + "/responsible", adminToken,
                "{\"assignedEmployeeId\":" + employee + ",\"assignedEmployeeName\":\"Forged Name\",\"name\":\"Renamed\","
                        + "\"address\":\"Hacked Street 9\",\"cost\":1,\"isActive\":false,\"productIds\":[],\"tenantId\":"
                        + OTHER_TENANT + "}");

        assertEquals(200, response.statusCode());
        assertEquals(name, json(response).get("name").asText());
        assertEquals("Dorte Dam", json(response).get("assignedEmployeeName").asText());
        assertEquals(TENANT, json(response).get("tenantId").asLong());
        assertTrue(json(response).get("isActive").asBoolean());
        assertDetailsUntouched(json(response), product, otherProduct);
        assertDbDetailsEqual(before, dbDetails(id));
        assertEquals(List.of(product, otherProduct), dbProducts(id));
    }

    @Test
    void deleteRemovesTheResponsiblePersonWithoutTouchingAnythingElse() throws Exception {
        long product = productId(TENANT);
        long otherProduct = productId(TENANT);
        long id = createWithDetails(unique("Detailed"), product, otherProduct);
        Object[] before = dbDetails(id);
        long employee = employeeId(TENANT, "Erik Eriksen", true);
        assertEquals(200, setResponsible(adminToken, id, employee).statusCode());

        HttpResponse<String> response = send("DELETE", "/api/assignment/" + id + "/responsible", adminToken, null);

        assertEquals(200, response.statusCode());
        assertTrue(json(response).get("assignedEmployeeId").isNull());
        assertTrue(json(response).get("assignedEmployeeName").isNull());
        assertDetailsUntouched(json(response), product, otherProduct);
        assertNull(dbEmployee(id));
        assertDbDetailsEqual(before, dbDetails(id));
        assertEquals(List.of(product, otherProduct), dbProducts(id));
        assertNotNull(new UserDAO(emf).getById(employee));

        HttpResponse<String> again = send("DELETE", "/api/assignment/" + id + "/responsible", adminToken, null);
        assertEquals(200, again.statusCode());
        assertTrue(json(again).get("assignedEmployeeId").isNull());
        assertEquals(1, dbCount(id));
    }

    @Test
    void theFullUpdateStillSetsAndClearsTheResponsiblePerson() throws Exception {
        String name = unique("Full update");
        long id = create(name);
        long employee = employeeId(TENANT, "Freja Frost", true);

        HttpResponse<String> updated = send("PUT", "/api/assignment/" + id, adminToken,
                "{\"name\":\"" + name + "\",\"address\":\"Main Street 1\",\"assignedEmployeeId\":" + employee
                        + ",\"assignedEmployeeName\":\"Forged Name\",\"version\":0}");
        assertEquals(200, updated.statusCode());
        assertEquals("Main Street 1", json(updated).get("address").asText());
        assertEquals(employee, json(updated).get("assignedEmployeeId").asLong());
        assertEquals("Freja Frost", json(updated).get("assignedEmployeeName").asText());

        HttpResponse<String> cleared = send("PUT", "/api/assignment/" + id, adminToken,
                "{\"name\":\"" + name + "\",\"version\":1}");
        assertEquals(200, cleared.statusCode());
        assertTrue(json(cleared).get("assignedEmployeeId").isNull());
        assertTrue(json(cleared).get("assignedEmployeeName").isNull());
        assertNull(dbEmployee(id));
    }

    @Test
    void anEmployeeFromAnotherTenantIsRejected() throws Exception {
        long id = create(unique("Cleaning"));
        long current = employeeId(TENANT, "Current Person", true);
        long foreign = employeeId(OTHER_TENANT, "Foreign Person", true);
        assertEquals(200, setResponsible(adminToken, id, current).statusCode());

        HttpResponse<String> response = setResponsible(adminToken, id, foreign);

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Employee not found"));
        assertFalse(response.body().contains("Foreign Person"));
        assertEquals(current, dbEmployee(id));
    }

    @Test
    void aResponsiblePersonWhoIsDeactivatedLaterMayStayButCannotBeBroughtBack() throws Exception {
        long id = create(unique("Cleaning"));
        long employee = employeeId(TENANT, "Gitte Gram", true);
        long replacement = employeeId(TENANT, "Hans Holm", true);
        assertEquals(200, setResponsible(adminToken, id, employee).statusCode());
        new UserDAO(emf).reversActivation(employee);

        assertEquals(employee, json(send("GET", "/api/assignment/" + id, adminToken, null)).get("assignedEmployeeId").asLong());
        assertEquals(200, setResponsible(adminToken, id, employee).statusCode());
        assertEquals(employee, dbEmployee(id));

        assertEquals(200, setResponsible(adminToken, id, replacement).statusCode());
        assertEquals(400, setResponsible(adminToken, id, employee).statusCode());
        assertEquals(replacement, dbEmployee(id));
    }

    @Test
    void aNonAdministratorCannotSetOrRemoveTheResponsiblePerson() throws Exception {
        long id = create(unique("Protected"));
        long current = employeeId(TENANT, "Current Person", true);
        long other = employeeId(TENANT, "Other Person", true);
        assertEquals(200, setResponsible(adminToken, id, current).statusCode());

        assertEquals(403, setResponsible(userToken, id, other).statusCode());
        assertEquals(403, send("DELETE", "/api/assignment/" + id + "/responsible", userToken, null).statusCode());
        assertEquals(401, setResponsible(null, id, other).statusCode());
        assertEquals(401, send("DELETE", "/api/assignment/" + id + "/responsible", null, null).statusCode());

        assertEquals(current, dbEmployee(id));
    }

    @Test
    void anAdministratorOfAnotherTenantGetsNotFound() throws Exception {
        long id = create(unique("Private"));
        long current = employeeId(TENANT, "Current Person", true);
        long theirEmployee = employeeId(OTHER_TENANT, "Their Person", true);
        assertEquals(200, setResponsible(adminToken, id, current).statusCode());

        assertEquals(404, setResponsible(otherTenantAdminToken, id, theirEmployee).statusCode());
        assertEquals(404, send("DELETE", "/api/assignment/" + id + "/responsible", otherTenantAdminToken, null).statusCode());
        assertEquals(404, setResponsible(adminToken, 999_999_999L, current).statusCode());
        assertEquals(404, send("DELETE", "/api/assignment/999999999/responsible", adminToken, null).statusCode());

        assertEquals(current, dbEmployee(id));
    }

    @Test
    void theResponsiblePersonIsARealForeignKeyToUsers() {
        try (EntityManager em = emf.createEntityManager()) {
            Number foreignKeys = (Number) em.createNativeQuery(
                            "SELECT COUNT(*) FROM pg_constraint WHERE contype = 'f'"
                                    + " AND conrelid = 'assignments'::regclass AND confrelid = 'users'::regclass")
                    .getSingleResult();
            assertEquals(1, foreignKeys.intValue());
        }
    }

    @Test
    void deletingAUserWhoIsResponsibleKeepsTheAssignmentWithNoResponsiblePerson() throws Exception {
        String name = unique("Survivor");
        long product = productId(TENANT);
        long otherProduct = productId(TENANT);
        long id = createWithDetails(name, product, otherProduct);
        long untouched = create(unique("Untouched"));
        long employee = employeeId(TENANT, "Ida Iversen", true);
        long colleague = employeeId(TENANT, "Jens Jensen", true);
        assertEquals(200, setResponsible(adminToken, id, employee).statusCode());
        assertEquals(200, setResponsible(adminToken, untouched, colleague).statusCode());

        assertEquals(204, send("DELETE", "/api/user/" + employee, adminToken, null).statusCode());

        assertNull(new UserDAO(emf).getById(employee));
        assertEquals(1, dbCount(id));
        assertNull(dbEmployee(id));
        HttpResponse<String> read = send("GET", "/api/assignment/" + id, adminToken, null);
        assertEquals(200, read.statusCode());
        assertEquals(name, json(read).get("name").asText());
        assertTrue(json(read).get("assignedEmployeeId").isNull());
        assertTrue(json(read).get("assignedEmployeeName").isNull());
        assertDetailsUntouched(json(read), product, otherProduct);
        assertEquals(colleague, dbEmployee(untouched));
    }

    private static HttpResponse<String> setResponsible(String token, long assignmentId, long employeeId) throws Exception {
        return send("PUT", "/api/assignment/" + assignmentId + "/responsible", token,
                "{\"assignedEmployeeId\":" + employeeId + "}");
    }

    private static long create(String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", adminToken, "{\"name\":\"" + name + "\"}");
        assertEquals(201, response.statusCode());
        return json(response).get("id").asLong();
    }

    private static long createWithDetails(String name, long product, long otherProduct) throws Exception {
        HttpResponse<String> response = send("POST", "/api/assignment", adminToken, "{\"name\":\"" + name
                + "\",\"address\":\"Main Street 1\",\"startTime\":\"2026-10-05T14:30:00\",\"estimatedEndTime\":\"2026-10-05T16:00:00\","
                + "\"estimatedMinutes\":90,\"cost\":1250.5,\"productIds\":[" + product + "," + otherProduct + "]}");
        assertEquals(201, response.statusCode());
        return json(response).get("id").asLong();
    }

    private static void assertDetailsUntouched(JsonNode body, long product, long otherProduct) {
        assertEquals("Main Street 1", body.get("address").asText());
        assertEquals("2026-10-05T14:30:00", body.get("startTime").asText());
        assertEquals("2026-10-05T16:00:00", body.get("estimatedEndTime").asText());
        assertEquals(90, body.get("estimatedMinutes").asInt());
        assertEquals(0, new BigDecimal("1250.50").compareTo(body.get("cost").decimalValue()));
        assertTrue(body.get("isActive").asBoolean());
        assertEquals(2, body.get("productIds").size());
        assertEquals(product, body.get("productIds").get(0).asLong());
        assertEquals(otherProduct, body.get("productIds").get(1).asLong());
    }

    private static void assertDbDetailsEqual(Object[] before, Object[] after) {
        assertEquals(before.length, after.length);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i], after[i]);
        }
    }

    private static JsonNode inList(HttpResponse<String> response, long id) throws Exception {
        assertEquals(200, response.statusCode());
        for (JsonNode assignment : json(response)) {
            if (assignment.get("id").asLong() == id) {
                return assignment;
            }
        }
        throw new AssertionError("Assignment " + id + " is not in the list");
    }

    private static long employeeId(long tenantId, String name, boolean active) {
        User user = new User(null, UUID.randomUUID() + "@example.com", PASSWORD, null, tenantId, active, Set.of("USER"));
        user.setName(name);
        return new UserDAO(emf).create(user).getId();
    }

    private static long productId(long tenantId) {
        String number = unique("P");
        return new ProductDAO(emf).save(Product.builder()
                .tenantId(tenantId)
                .productNumber(number)
                .name("Product " + number)
                .idempotencyKey(number)
                .build()).getId();
    }

    private static Long dbEmployee(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            Number value = (Number) em.createNativeQuery("SELECT assigned_employee_id FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
            return value == null ? null : value.longValue();
        }
    }

    private static Object[] dbDetails(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return (Object[]) em.createNativeQuery(
                            "SELECT name, is_active, tenant_id, address, estimated_minutes, cost, start_time, estimated_end_time FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult();
        }
    }

    private static List<Long> dbProducts(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> rows = em.createNativeQuery(
                            "SELECT product_id FROM assignment_products WHERE assignment_id = :id ORDER BY product_order")
                    .setParameter("id", id)
                    .getResultList();
            return rows.stream().map(row -> ((Number) row).longValue()).toList();
        }
    }

    private static int dbCount(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult()).intValue();
        }
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
}

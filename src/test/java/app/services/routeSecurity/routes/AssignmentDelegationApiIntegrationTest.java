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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class AssignmentDelegationApiIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_delegation_test")
            .withUsername("test")
            .withPassword("test");

    private static final String PASSWORD = "secret-password";
    private static final long TENANT = 701L;
    private static final long OTHER_TENANT = 702L;

    private static EntityManagerFactory emf;
    private static ApplicationConfig applicationConfig;
    private static Javalin app;
    private static HttpClient httpClient;
    private static ObjectMapper objectMapper;
    private static Account admin;
    private static String userToken;
    private static String otherTenantAdminToken;

    private record Account(long id, String email, String token) {}

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

        admin = account("ADMIN", TENANT, "Admin Person");
        userToken = account("USER", TENANT, "Plain User").token();
        otherTenantAdminToken = account("ADMIN", OTHER_TENANT, "Other Admin").token();
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
    void anAdministratorCanDelegateOneAssignmentToSeveralEmployees() throws Exception {
        long id = create(unique("Cleaning"), null, null);
        long first = employeeId(TENANT, "Anna Andersen", true);
        long second = employeeId(TENANT, "Bo Berg", true);

        HttpResponse<String> firstResponse = delegate(admin.token(), id, first);
        HttpResponse<String> secondResponse = delegate(admin.token(), id, second);

        assertEquals(204, firstResponse.statusCode());
        assertEquals("", firstResponse.body());
        assertEquals(204, secondResponse.statusCode());
        assertEquals(List.of(first, second), dbDelegatedEmployees(id));

        HttpResponse<String> list = send("GET", "/api/assignment/" + id + "/delegations", admin.token(), null);
        assertEquals(200, list.statusCode());
        JsonNode body = json(list);
        assertEquals(2, body.size());
        assertEquals(first, body.get(0).get("employeeId").asLong());
        assertEquals("Anna Andersen", body.get(0).get("employeeName").asText());
        assertEquals(second, body.get(1).get("employeeId").asLong());

        JsonNode fromList = find(json(send("GET", "/api/assignment/all", admin.token(), null)), "id", String.valueOf(id));
        assertEquals(2, fromList.get("assignedEmployees").size());
        assertEquals("Anna Andersen", fromList.get("assignedEmployees").get(0).get("employeeName").asText());
        JsonNode single = json(send("GET", "/api/assignment/" + id, admin.token(), null));
        assertEquals(2, single.get("assignedEmployees").size());
    }

    @Test
    void theSystemRecordsWhoDelegatedAndWhen() throws Exception {
        long id = create(unique("Kitchen"), null, null);
        long employee = employeeId(TENANT, "Carl Clausen", true);

        assertEquals(204, delegate(admin.token(), id, employee).statusCode());

        JsonNode delegation = json(send("GET", "/api/assignment/" + id + "/delegations", admin.token(), null)).get(0);
        assertEquals(admin.id(), delegation.get("delegatedByUserId").asLong());
        assertEquals(admin.email(), delegation.get("delegatedBy").asText());
        assertNotNull(delegation.get("delegatedAt"));
        assertFalse(delegation.get("delegatedAt").isNull());

        JsonNode history = json(send("GET", "/api/assignment/" + id + "/history", admin.token(), null));
        JsonNode entry = find(history, "action", "DELEGATE");
        assertNotNull(entry);
        assertEquals(employee, entry.get("newEmployee").asLong());
        assertEquals(admin.email(), entry.get("changedBy").asText());
    }

    @Test
    void aDelegatedAssignmentAppearsInTheEmployeesSchedule() throws Exception {
        long id = create(unique("Visible"), "2031-03-01T09:00:00", "2031-03-01T11:00:00");
        Account employee = account("USER", TENANT, "Dorthe Dam");

        assertFalse(listContains(send("GET", "/api/assignment/all", employee.token(), null), id));
        assertEquals(404, send("GET", "/api/assignment/" + id, employee.token(), null).statusCode());

        assertEquals(204, delegate(admin.token(), id, employee.id()).statusCode());

        assertTrue(listContains(send("GET", "/api/assignment/all", employee.token(), null), id));
        HttpResponse<String> detail = send("GET", "/api/assignment/" + id, employee.token(), null);
        assertEquals(200, detail.statusCode());
        assertTrue(json(detail).get("cost").isNull());
    }

    @Test
    void anEmployeeCannotBeDelegatedTwiceOrAlongsideBeingResponsible() throws Exception {
        long id = create(unique("Twice"), null, null);
        long delegated = employeeId(TENANT, "Erik Eriksen", true);
        long responsible = employeeId(TENANT, "Frida Friis", true);
        assertEquals(200, send("PUT", "/api/assignment/" + id + "/responsible", admin.token(),
                "{\"assignedEmployeeId\":" + responsible + "}").statusCode());

        assertEquals(204, delegate(admin.token(), id, delegated).statusCode());
        assertEquals(409, delegate(admin.token(), id, delegated).statusCode());
        assertEquals(409, delegate(admin.token(), id, responsible).statusCode());
        assertEquals(List.of(delegated), dbDelegatedEmployees(id));
    }

    @Test
    void invalidEmployeesAreRejectedWithoutChangingTheAssignment() throws Exception {
        long id = create(unique("Invalid employee"), null, null);
        long inactive = employeeId(TENANT, "Inactive Person", false);
        long foreign = employeeId(OTHER_TENANT, "Foreign Person", true);
        long version = dbVersion(id);

        assertEquals(409, delegate(admin.token(), id, inactive).statusCode());
        assertEquals(400, delegate(admin.token(), id, foreign).statusCode());
        assertEquals(400, delegate(admin.token(), id, 999_999_999L).statusCode());
        assertEquals(400, send("POST", "/api/assignment/" + id + "/delegations", admin.token(), "{}").statusCode());

        assertEquals(List.of(), dbDelegatedEmployees(id));
        assertEquals(version, dbVersion(id));
        assertTrue(find(json(send("GET", "/api/assignment/" + id + "/history", admin.token(), null)), "action", "DELEGATE") == null);
    }

    @Test
    void onlyActivePlannedAssignmentsCanBeDelegated() throws Exception {
        long employee = employeeId(TENANT, "Gitte Gram", true);
        long inactive = create(unique("Inactive"), null, null);
        assertEquals(200, send("PATCH", "/api/assignment/" + inactive + "/deactivate", admin.token(), null).statusCode());
        long cancelled = create(unique("Cancelled"), null, null);
        assertEquals(200, send("PATCH", "/api/assignment/" + cancelled + "/state", admin.token(),
                "{\"state\":\"CANCELLED\"}").statusCode());

        assertEquals(409, delegate(admin.token(), inactive, employee).statusCode());
        assertEquals(409, delegate(admin.token(), cancelled, employee).statusCode());
        assertEquals(List.of(), dbDelegatedEmployees(inactive));
        assertEquals(List.of(), dbDelegatedEmployees(cancelled));
    }

    @Test
    void delegationRejectsOverlappingWorkForTheEmployee() throws Exception {
        long employee = employeeId(TENANT, "Hans Holm", true);
        long morning = create(unique("Morning"), "2031-04-01T08:00:00", "2031-04-01T12:00:00");
        long overlapping = create(unique("Overlapping"), "2031-04-01T11:00:00", "2031-04-01T13:00:00");
        long afternoon = create(unique("Afternoon"), "2031-04-01T12:00:00", "2031-04-01T16:00:00");
        assertEquals(204, delegate(admin.token(), morning, employee).statusCode());

        HttpResponse<String> conflict = delegate(admin.token(), overlapping, employee);

        assertEquals(409, conflict.statusCode());
        assertTrue(conflict.body().contains(String.valueOf(morning)));
        assertEquals(List.of(), dbDelegatedEmployees(overlapping));
        assertEquals(204, delegate(admin.token(), afternoon, employee).statusCode());
    }

    @Test
    void delegationRejectsOverlapWithWorkTheEmployeeIsResponsibleFor() throws Exception {
        long employee = employeeId(TENANT, "Ida Iversen", true);
        long responsibleFor = create(unique("Responsible"), "2031-05-01T08:00:00", "2031-05-01T12:00:00");
        assertEquals(200, send("PUT", "/api/assignment/" + responsibleFor + "/responsible", admin.token(),
                "{\"assignedEmployeeId\":" + employee + "}").statusCode());
        long overlapping = create(unique("Clash"), "2031-05-01T10:00:00", "2031-05-01T14:00:00");

        assertEquals(409, delegate(admin.token(), overlapping, employee).statusCode());
        assertEquals(List.of(), dbDelegatedEmployees(overlapping));
    }

    @Test
    void anAdministratorCanRemoveADelegation() throws Exception {
        long id = create(unique("Remove"), null, null);
        Account employee = account("USER", TENANT, "Jens Juhl");
        long other = employeeId(TENANT, "Karen Krog", true);
        assertEquals(204, delegate(admin.token(), id, employee.id()).statusCode());
        assertEquals(204, delegate(admin.token(), id, other).statusCode());

        HttpResponse<String> removed = send("DELETE", "/api/assignment/" + id + "/delegations/" + employee.id(), admin.token(), null);

        assertEquals(204, removed.statusCode());
        assertEquals(List.of(other), dbDelegatedEmployees(id));
        assertFalse(listContains(send("GET", "/api/assignment/all", employee.token(), null), id));
        assertEquals(404, send("DELETE", "/api/assignment/" + id + "/delegations/" + employee.id(), admin.token(), null).statusCode());
        assertNotNull(find(json(send("GET", "/api/assignment/" + id + "/history", admin.token(), null)), "action", "UNDELEGATE"));
    }

    @Test
    void onlyAdministratorsOfTheSameCompanyCanDelegate() throws Exception {
        long id = create(unique("Protected"), null, null);
        long employee = employeeId(TENANT, "Lone Lund", true);

        assertEquals(403, delegate(userToken, id, employee).statusCode());
        assertEquals(404, delegate(otherTenantAdminToken, id, employee).statusCode());
        assertEquals(404, send("GET", "/api/assignment/" + id + "/delegations", otherTenantAdminToken, null).statusCode());
        assertEquals(List.of(), dbDelegatedEmployees(id));
    }

    @Test
    void anAssignedEmployeeWhoBecomesResponsibleIsNotListedTwice() throws Exception {
        long id = create(unique("Promote"), null, null);
        long first = employeeId(TENANT, "Nina Nord", true);
        long promoted = employeeId(TENANT, "Ole Olsen", true);
        long other = employeeId(TENANT, "Pia Poulsen", true);
        assertEquals(200, send("PUT", "/api/assignment/" + id + "/responsible", admin.token(),
                "{\"assignedEmployeeId\":" + first + "}").statusCode());
        assertEquals(204, delegate(admin.token(), id, promoted).statusCode());
        assertEquals(204, delegate(admin.token(), id, other).statusCode());

        HttpResponse<String> changed = send("PUT", "/api/assignment/" + id + "/responsible", admin.token(),
                "{\"assignedEmployeeId\":" + promoted + "}");

        assertEquals(200, changed.statusCode());
        assertEquals(promoted, json(changed).get("assignedEmployeeId").asLong());
        assertEquals(1, json(changed).get("assignedEmployees").size());
        assertEquals(other, json(changed).get("assignedEmployees").get(0).get("employeeId").asLong());
        assertEquals(List.of(other), dbDelegatedEmployees(id));
    }

    @Test
    void aDelegatedAssignmentCanStillBeDeleted() throws Exception {
        long id = create(unique("Delete me"), null, null);
        assertEquals(204, delegate(admin.token(), id, employeeId(TENANT, "Mads Mik", true)).statusCode());

        assertEquals(204, send("DELETE", "/api/assignment/" + id, admin.token(), null).statusCode());
        assertEquals(List.of(), dbDelegatedEmployees(id));
    }

    private static HttpResponse<String> delegate(String token, long assignmentId, long employeeId) throws Exception {
        return send("POST", "/api/assignment/" + assignmentId + "/delegations", token,
                "{\"employeeId\":" + employeeId + "}");
    }

    private static long create(String name, String startTime, String estimatedEndTime) throws Exception {
        String times = startTime == null ? ""
                : ",\"startTime\":\"" + startTime + "\",\"estimatedEndTime\":\"" + estimatedEndTime + "\"";
        HttpResponse<String> response = send("POST", "/api/assignment", admin.token(),
                "{\"name\":\"" + name + "\",\"cost\":100" + times + "}");
        assertEquals(201, response.statusCode(), response.body());
        return json(response).get("id").asLong();
    }

    private static long employeeId(long tenantId, String name, boolean active) {
        User user = new User(null, UUID.randomUUID() + "@example.com", PASSWORD, null, tenantId, active, Set.of("USER"));
        user.setName(name);
        return new UserDAO(emf).create(user).getId();
    }

    private static Account account(String role, long tenantId, String name) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        User user = new User(null, email, PASSWORD, null, tenantId, true, Set.of(role));
        user.setName(name);
        long id = new UserDAO(emf).create(user).getId();

        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, login.statusCode());
        return new Account(id, email, json(login).get("token").asText());
    }

    private static boolean listContains(HttpResponse<String> response, long id) throws Exception {
        assertEquals(200, response.statusCode());
        for (JsonNode item : json(response)) {
            if (item.get("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode find(JsonNode list, String field, String value) {
        for (JsonNode item : list) {
            if (value.equals(item.get(field).asText())) {
                return item;
            }
        }
        return null;
    }

    private static List<Long> dbDelegatedEmployees(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            List<?> rows = em.createNativeQuery(
                            "SELECT employee_id FROM assignment_delegations WHERE assignment_id = :id ORDER BY delegated_at, id")
                    .setParameter("id", id)
                    .getResultList();
            List<Long> ids = new ArrayList<>();
            rows.forEach(row -> ids.add(((Number) row).longValue()));
            return ids;
        }
    }

    private static long dbVersion(long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return ((Number) em.createNativeQuery("SELECT version FROM assignments WHERE id = :id")
                    .setParameter("id", id)
                    .getSingleResult()).longValue();
        }
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
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

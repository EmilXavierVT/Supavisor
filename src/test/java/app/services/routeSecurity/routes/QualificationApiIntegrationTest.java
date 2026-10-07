package app.services.routeSecurity.routes;

import app.config.ApplicationConfig;
import app.config.TestEntityManagerFactory;
import app.dao.UserDAO;
import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.Tenant;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class QualificationApiIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_qualifications_test")
            .withUsername("test")
            .withPassword("test");

    private static final String PASSWORD = "secret-password";

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
    void administratorCanCreateAssignAndRemoveEmployeeQualification() throws Exception {
        Tenant tenant = createTenant();
        String adminToken = tokenFor("ADMIN", tenant.getId());
        User employee = employee(tenant.getId(), "Kitchen");
        long qualificationId = createQualification(adminToken, "Driver license");

        HttpResponse<String> assigned = send("POST",
                "/api/user/" + employee.getId() + "/qualifications/" + qualificationId,
                adminToken,
                null);

        assertEquals(200, assigned.statusCode());
        assertEquals("Kitchen", json(assigned).get("primaryCategory").asText());
        assertEquals(Set.of("Driver license"), qualificationNamesFrom(json(assigned)));
        assertEquals(Set.of("Driver license"), dbQualificationNames(employee.getId()));

        HttpResponse<String> duplicate = send("POST",
                "/api/user/" + employee.getId() + "/qualifications/" + qualificationId,
                adminToken,
                null);

        assertEquals(409, duplicate.statusCode());
        assertEquals(Set.of("Driver license"), dbQualificationNames(employee.getId()));

        HttpResponse<String> removed = send("DELETE",
                "/api/user/" + employee.getId() + "/qualifications/" + qualificationId,
                adminToken,
                null);

        assertEquals(200, removed.statusCode());
        assertTrue(qualificationNamesFrom(json(removed)).isEmpty());
        assertEquals("Kitchen", json(removed).get("primaryCategory").asText());
    }

    @Test
    void removingQualificationDoesNotEraseExistingAssignmentRecords() throws Exception {
        Tenant tenant = createTenant();
        String adminToken = tokenFor("ADMIN", tenant.getId());
        User employee = employee(tenant.getId(), "Cleaning");
        long qualificationId = createQualification(adminToken, "Food safety");
        assignQualification(adminToken, employee.getId(), qualificationId);
        long assignmentId = assignmentFor(tenant.getId(), employee.getId());

        HttpResponse<String> removed = send("DELETE",
                "/api/user/" + employee.getId() + "/qualifications/" + qualificationId,
                adminToken,
                null);
        HttpResponse<String> assignment = send("GET", "/api/assignment/" + assignmentId, adminToken, null);

        assertEquals(200, removed.statusCode());
        assertEquals(200, assignment.statusCode());
        assertEquals(employee.getId(), json(assignment).get("assignedEmployeeId").asLong());
        assertTrue(json(assignment).get("assignedEmployeeQualifications").isEmpty());
    }

    @Test
    void nonAdministratorCannotChangeQualifications() throws Exception {
        Tenant tenant = createTenant();
        String adminToken = tokenFor("ADMIN", tenant.getId());
        String userToken = tokenFor("USER", tenant.getId());
        User employee = employee(tenant.getId(), null);
        long qualificationId = createQualification(adminToken, "Forklift");

        HttpResponse<String> response = send("POST",
                "/api/user/" + employee.getId() + "/qualifications/" + qualificationId,
                userToken,
                null);

        assertEquals(403, response.statusCode());
        assertTrue(dbQualificationNames(employee.getId()).isEmpty());
    }

    @Test
    void qualificationListIsTenantScoped() throws Exception {
        Tenant mine = createTenant();
        Tenant other = createTenant();
        String myAdminToken = tokenFor("ADMIN", mine.getId());
        String otherAdminToken = tokenFor("ADMIN", other.getId());
        createQualification(myAdminToken, "Catering");
        createQualification(otherAdminToken, "Cleaning");

        HttpResponse<String> response = send("GET", "/api/qualification/all", myAdminToken, null);

        assertEquals(200, response.statusCode());
        assertEquals(1, json(response).size());
        assertEquals("Catering", json(response).get(0).get("name").asText());
    }

    private static long createQualification(String token, String name) throws Exception {
        HttpResponse<String> response = send("POST", "/api/qualification/", token, "{\"name\":\"" + name + "\"}");
        assertEquals(201, response.statusCode());
        return json(response).get("id").asLong();
    }

    private static void assignQualification(String token, Long userId, Long qualificationId) throws Exception {
        assertEquals(200, send("POST", "/api/user/" + userId + "/qualifications/" + qualificationId, token, null).statusCode());
    }

    private static User employee(Long tenantId, String primaryCategory) {
        User user = new User(null, UUID.randomUUID() + "@example.com", PASSWORD, null,
                tenantId, true, Set.of("USER"));
        user.setPrimaryCategory(primaryCategory);
        return userDAO.create(user);
    }

    private static long assignmentFor(Long tenantId, Long employeeId) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User employee = em.find(User.class, employeeId);
            Assignment assignment = new Assignment(null, "Assignment " + UUID.randomUUID(), tenantId, true);
            assignment.setState(AssignmentState.PLANNED);
            assignment.setAssignedEmployee(employee);
            em.persist(assignment);
            em.getTransaction().commit();
            return assignment.getId();
        }
    }

    private static Tenant createTenant() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant tenant = new Tenant(null, "tenant-" + UUID.randomUUID());
            em.persist(tenant);
            em.getTransaction().commit();
            return tenant;
        }
    }

    private static String tokenFor(String role, long tenantId) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        userDAO.create(new User(null, email, PASSWORD, null, tenantId, true, Set.of(role)));

        HttpResponse<String> login = send("POST", "/api/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, login.statusCode());
        return json(login).get("token").asText();
    }

    private static Set<String> dbQualificationNames(Long userId) {
        User user = userDAO.getById(userId);
        Set<String> names = new java.util.HashSet<>();
        user.getQualifications().forEach(qualification -> names.add(qualification.getName()));
        return names;
    }

    private static Set<String> qualificationNamesFrom(JsonNode user) {
        Set<String> names = new java.util.HashSet<>();
        user.get("qualifications").forEach(node -> names.add(node.get("name").asText()));
        return names;
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

package app.services.routeSecurity.routes;

import app.config.ApplicationConfig;
import app.config.TestEntityManagerFactory;
import app.dao.RoleDAO;
import app.dao.UserDAO;
import app.entities.Role;
import app.entities.Tenant;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PUT /user/{id}/roles, exercised over real HTTP against a real Postgres. */
@Testcontainers
class UserRolesApiIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_user_roles_test")
            .withUsername("test")
            .withPassword("test");

    private static final String PASSWORD = "secret-password";

    private static EntityManagerFactory emf;
    private static ApplicationConfig applicationConfig;
    private static Javalin app;
    private static HttpClient httpClient;
    private static ObjectMapper objectMapper;
    private static UserDAO userDAO;
    private static RoleDAO roleDAO;

    @BeforeAll
    static void setUp() {
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
        userDAO = new UserDAO(emf);
        roleDAO = new RoleDAO(emf);
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
    void movingFromOneCustomRoleToAnotherDropsTheOldOne() throws Exception {
        Tenant tenant = createTenant();
        Role kitchen = createRole(tenant, "Kitchen");
        Role cleaning = createRole(tenant, "Cleaning");
        String adminToken = tokenFor("ADMIN", tenant.getId());
        User employee = employee(tenant.getId());

        assertEquals(200, sendRoles(employee.getId(), adminToken, Set.of(kitchen.getId())).statusCode());
        assertEquals(Set.of("Kitchen"), dbRoleNames(employee.getId()));

        HttpResponse<String> moved = sendRoles(employee.getId(), adminToken, Set.of(cleaning.getId()));

        assertEquals(200, moved.statusCode());
        assertEquals(Set.of("Cleaning"), roleNamesFrom(moved));
        assertEquals(Set.of("Cleaning"), dbRoleNames(employee.getId()));
    }

    @Test
    void anEmptySetClearsAllCustomRoles() throws Exception {
        Tenant tenant = createTenant();
        Role kitchen = createRole(tenant, "Kitchen");
        String adminToken = tokenFor("ADMIN", tenant.getId());
        User employee = employee(tenant.getId());
        sendRoles(employee.getId(), adminToken, Set.of(kitchen.getId()));

        HttpResponse<String> cleared = sendRoles(employee.getId(), adminToken, Set.of());

        assertEquals(200, cleared.statusCode());
        assertTrue(dbRoleNames(employee.getId()).isEmpty());
    }

    @Test
    void aRoleFromAnotherTenantIsRejectedAndNothingChanges() throws Exception {
        Tenant mine = createTenant();
        Tenant other = createTenant();
        Role kitchen = createRole(mine, "Kitchen");
        Role foreign = createRole(other, "Foreign");
        String adminToken = tokenFor("ADMIN", mine.getId());
        User employee = employee(mine.getId());
        sendRoles(employee.getId(), adminToken, Set.of(kitchen.getId()));

        HttpResponse<String> response = sendRoles(employee.getId(), adminToken, Set.of(foreign.getId()));

        assertEquals(400, response.statusCode());
        assertEquals(Set.of("Kitchen"), dbRoleNames(employee.getId()));
    }

    @Test
    void anotherTenantsAdministratorCannotSetRolesForThisUser() throws Exception {
        Tenant mine = createTenant();
        Tenant other = createTenant();
        Role kitchen = createRole(mine, "Kitchen");
        User employee = employee(mine.getId());
        String otherAdminToken = tokenFor("ADMIN", other.getId());

        assertEquals(404, sendRoles(employee.getId(), otherAdminToken, Set.of(kitchen.getId())).statusCode());
        assertTrue(dbRoleNames(employee.getId()).isEmpty());
    }

    @Test
    void aNonAdministratorCannotSetRoles() throws Exception {
        Tenant tenant = createTenant();
        Role kitchen = createRole(tenant, "Kitchen");
        String userToken = tokenFor("USER", tenant.getId());
        User employee = employee(tenant.getId());

        assertEquals(403, sendRoles(employee.getId(), userToken, Set.of(kitchen.getId())).statusCode());
        assertTrue(dbRoleNames(employee.getId()).isEmpty());
    }

    // ---- helpers

    private static HttpResponse<String> sendRoles(Long employeeId, String token, Set<Long> customRoleIds) throws Exception {
        String ids = customRoleIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        return send("PUT", "/api/user/" + employeeId + "/roles", token, "{\"customRoleIds\":[" + ids + "]}");
    }

    private static User employee(Long tenantId) {
        return userDAO.create(new User(null, UUID.randomUUID() + "@example.com", PASSWORD, null,
                tenantId, true, Set.of("USER")));
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

    private static Role createRole(Tenant tenant, String name) {
        return roleDAO.save(Role.builder().roleName(name).tenant(tenant).build());
    }

    private static Set<String> dbRoleNames(Long userId) {
        User user = userDAO.getById(userId);
        Set<String> names = new java.util.HashSet<>();
        user.getCustomRoles().forEach(role -> names.add(role.getRoleName()));
        return names;
    }

    private static Set<String> roleNamesFrom(HttpResponse<String> response) throws Exception {
        JsonNode customRoles = json(response).get("customRoles");
        Set<String> names = new java.util.HashSet<>();
        customRoles.forEach(node -> names.add(node.get("roleName").asText()));
        return names;
    }

    private static String tokenFor(String role, long tenantId) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        userDAO.create(new User(null, email, PASSWORD, null, tenantId, true, Set.of(role)));

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

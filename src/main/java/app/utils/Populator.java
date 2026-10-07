package app.utils;

import app.config.HibernateConfig;
import app.entities.Assignment;
import app.entities.AssignmentState;
import app.entities.AssignmentStateHistory;
import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Fills the configured database with demo data: 10 tenants, 100 users each (3 admins), a set of
 * custom roles per tenant and one week (Monday-Sunday of the current week) of assignments.
 * <p>
 * Assignments before "now" are finished (mostly COMPLETED, with check-in/out), the ones running now
 * are IN_PROGRESS and the rest are PLANNED/ACKNOWLEDGED/... - each with matching state history.
 * <p>
 * Tenants that already exist (by name) are skipped, so it is safe to run more than once.
 * Every populated user has the password {@value #PASSWORD}, except the fixed test login
 * {@value #TEST_EMAIL} / {@value #TEST_PASSWORD}, which is the first admin of the first tenant
 * (left alone if that email already exists).
 * <p>
 * Run the main method from the IDE, or: mvn compile exec:java -Dexec.mainClass=app.utils.Populator
 */
public class Populator {

    public static final String PASSWORD = "Password123!";
    public static final String TEST_EMAIL = "test@test.dk";
    public static final String TEST_PASSWORD = "1234";
    private static final String SOURCE = "populator";
    private static final int USERS_PER_TENANT = 100;
    private static final int ADMINS_PER_TENANT = 3;
    private static final int WEEKDAY_ASSIGNMENTS = 20;
    private static final int WEEKEND_ASSIGNMENTS = 8;
    private static final ZoneId ZONE = ZoneId.of("Europe/Copenhagen");
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("EEE dd/MM HH:mm", Locale.ENGLISH);

    private static final String[] TENANTS = {
            "Nordic Clean ApS", "Hygge Facility Service", "Københavns Rengøring", "Aarhus Kantine & Køkken",
            "Fyn Vinduespolering", "Jysk Ejendomsservice", "Øresund Facility", "Skagen Service Partner",
            "Vestegnens Rengøring", "Bornholm Drift & Pleje"
    };

    private static final String[] CUSTOM_ROLES = {
            "Cleaning", "Kitchen", "Window cleaning", "Maintenance", "Driver", "Supervisor"
    };

    /** Task type -> custom role an employee needs to get it, base duration (minutes) and price. */
    private record TaskType(String name, String role, int minutes, int price) {}

    private static final TaskType[] TASKS = {
            new TaskType("Office cleaning", "Cleaning", 120, 850),
            new TaskType("Stairwell cleaning", "Cleaning", 60, 450),
            new TaskType("Deep cleaning", "Cleaning", 240, 1900),
            new TaskType("Lunch service", "Kitchen", 180, 1400),
            new TaskType("Kitchen prep", "Kitchen", 120, 900),
            new TaskType("Window cleaning", "Window cleaning", 90, 700),
            new TaskType("Facade windows", "Window cleaning", 180, 1600),
            new TaskType("Maintenance check", "Maintenance", 60, 600),
            new TaskType("Repair visit", "Maintenance", 120, 1100),
            new TaskType("Supply delivery", "Driver", 45, 350),
            new TaskType("Quality inspection", "Supervisor", 60, 500)
    };

    private static final String[] FIRST_NAMES = {
            "Anders", "Mette", "Lars", "Sofie", "Jens", "Ida", "Mikkel", "Freja", "Rasmus", "Emma",
            "Frederik", "Clara", "Mads", "Laura", "Søren", "Anna", "Christian", "Maja", "Emil", "Julie",
            "Mohammed", "Fatima", "Oliver", "Karoline", "Lukas", "Signe", "Ahmed", "Nanna", "Jonas", "Amalie"
    };

    private static final String[] LAST_NAMES = {
            "Jensen", "Nielsen", "Hansen", "Pedersen", "Andersen", "Christensen", "Larsen", "Sørensen",
            "Rasmussen", "Jørgensen", "Petersen", "Madsen", "Kristensen", "Olsen", "Thomsen", "Poulsen",
            "Johansen", "Møller", "Mortensen", "Ali", "Khan", "Holm", "Lund", "Dahl"
    };

    private static final String[] STREETS = {
            "Vesterbrogade", "Nørrebrogade", "Strandvejen", "Østergade", "Algade", "Bredgade", "Søndergade",
            "Jernbanegade", "Havnegade", "Kongensgade", "Torvegade", "Skolevej", "Industrivej", "Parkvej"
    };

    private static final String[] CITIES = {
            "1620 København V", "2200 København N", "2900 Hellerup", "8000 Aarhus C", "5000 Odense C",
            "9000 Aalborg", "6700 Esbjerg", "7100 Vejle", "4000 Roskilde", "3700 Rønne"
    };

    private static final int[] START_HOURS = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};

    private final EntityManagerFactory emf;
    private final Random random = new Random(42);
    private final String passwordHash = BCrypt.hashpw(PASSWORD, BCrypt.gensalt());
    private final LocalDateTime now = LocalDateTime.now(ZONE);
    private final LocalDate monday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

    public Populator(EntityManagerFactory emf) {
        this.emf = emf;
    }

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        try {
            new Populator(emf).populate();
        } finally {
            emf.close();
        }
    }

    public void populate() {
        for (int i = 0; i < TENANTS.length; i++) {
            String tenantName = TENANTS[i];
            if (tenantExists(tenantName)) {
                System.out.println("Skipping \"" + tenantName + "\" - it already exists");
                continue;
            }
            populateTenant(tenantName, i + 1);
        }
        System.out.println("Done. Every populated user has the password " + PASSWORD
                + " (except " + TEST_EMAIL + " / " + TEST_PASSWORD + ")");
    }

    private boolean tenantExists(String name) {
        try (EntityManager em = emf.createEntityManager()) {
            return !em.createQuery("SELECT t.id FROM Tenant t WHERE t.name = :name", Long.class)
                    .setParameter("name", name)
                    .getResultList()
                    .isEmpty();
        }
    }

    private void populateTenant(String tenantName, int tenantNumber) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Tenant tenant = Tenant.builder().name(tenantName).build();
            em.persist(tenant);

            Map<String, Role> roles = new HashMap<>();
            for (String roleName : CUSTOM_ROLES) {
                Role role = Role.builder().roleName(roleName).tenant(tenant).build();
                em.persist(role);
                roles.put(roleName, role);
            }

            String domain = "tenant" + tenantNumber + ".dk";
            boolean addTestUser = tenantNumber == 1 && !emailExists(em, TEST_EMAIL);
            if (tenantNumber == 1 && !addTestUser) {
                System.out.println(TEST_EMAIL + " already exists - leaving that user as it is");
            }
            Map<String, List<User>> employeesByRole = new HashMap<>();
            List<User> admins = new ArrayList<>();
            for (int u = 1; u <= USERS_PER_TENANT; u++) {
                boolean testUser = addTestUser && u == 1;
                User user = createUser(em, tenant, roles, domain, u, testUser, employeesByRole);
                if (u <= ADMINS_PER_TENANT) admins.add(user);
            }

            int assignments = createWeekOfAssignments(em, tenant, employeesByRole);

            em.getTransaction().commit();
            System.out.printf("Created \"%s\" (id %d): %d users, %d roles, %d assignments. Admin login: %s%n",
                    tenantName, tenant.getId(), USERS_PER_TENANT, roles.size(), assignments, admins.get(0).getEmail());
        }
    }

    private boolean emailExists(EntityManager em, String email) {
        return !em.createQuery("SELECT u.id FROM User u WHERE LOWER(u.email) = LOWER(:email)", Long.class)
                .setParameter("email", email)
                .getResultList()
                .isEmpty();
    }

    private User createUser(EntityManager em, Tenant tenant, Map<String, Role> roles, String domain, int number,
                            boolean testUser, Map<String, List<User>> employeesByRole) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        boolean admin = number <= ADMINS_PER_TENANT;
        String email = testUser ? TEST_EMAIL
                : admin ? "admin" + number + "@" + domain
                : asciiLower(first) + "." + asciiLower(last) + number + "@" + domain;
        String hash = testUser ? BCrypt.hashpw(TEST_PASSWORD, BCrypt.gensalt()) : passwordHash;

        User user = new User(null, email, hash, randomPhone(), tenant.getId(), true,
                Set.of(admin ? "ADMIN" : "USER"));
        user.setName(first + " " + last);

        if (admin) {
            user.addCustomRole(roles.get("Supervisor"));
        } else {
            // one main skill, sometimes a second one
            String main = CUSTOM_ROLES[random.nextInt(CUSTOM_ROLES.length - 1)];
            user.addCustomRole(roles.get(main));
            if (random.nextInt(4) == 0) {
                user.addCustomRole(roles.get(pick(CUSTOM_ROLES)));
            }
        }
        em.persist(user);

        for (Role role : user.getCustomRoles()) {
            employeesByRole.computeIfAbsent(role.getRoleName(), k -> new ArrayList<>()).add(user);
        }
        return user;
    }

    private int createWeekOfAssignments(EntityManager em, Tenant tenant, Map<String, List<User>> employeesByRole) {
        int count = 0;
        for (int day = 0; day < 7; day++) {
            LocalDate date = monday.plusDays(day);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            int perDay = weekend ? WEEKEND_ASSIGNMENTS : WEEKDAY_ASSIGNMENTS;

            for (int a = 0; a < perDay; a++) {
                TaskType task = pick(TASKS);
                String address = pick(STREETS) + " " + (1 + random.nextInt(150)) + ", " + pick(CITIES);
                LocalDateTime start = date.atTime(START_HOURS[random.nextInt(START_HOURS.length)], random.nextInt(4) * 15);
                int minutes = task.minutes() + random.nextInt(4) * 15;
                LocalDateTime end = start.plusMinutes(minutes);
                List<User> candidates = employeesByRole.getOrDefault(task.role(), List.of());
                User employee = candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));

                // name is unique per tenant, so include where and when
                Assignment assignment = Assignment.builder()
                        .name(task.name() + " - " + address + " - " + start.format(NAME_TIME))
                        .tenantId(tenant.getId())
                        .address(address)
                        .estimatedMinutes(minutes)
                        .cost(BigDecimal.valueOf(task.price() + random.nextInt(10) * 50L))
                        .startTime(start)
                        .estimatedEndTime(end)
                        .assignedEmployeeId(employee == null ? null : employee.getId())
                        .build();
                em.persist(assignment);
                applyState(em, assignment, start, end);
                count++;
            }
        }
        return count;
    }

    /** Moves the assignment to a state that fits its time compared to now and records each step in the history. */
    private void applyState(EntityManager em, Assignment assignment, LocalDateTime start, LocalDateTime end) {
        Instant created = toInstant(start.minusDays(3 + random.nextInt(5)));
        Instant startInstant = toInstant(start);
        Instant endInstant = toInstant(end);
        addHistory(em, assignment, null, AssignmentState.PLANNED, created);

        int roll = random.nextInt(100);
        if (end.isBefore(now)) {
            if (roll < 80) {
                Instant checkIn = startInstant.plusSeconds(random.nextInt(15 * 60) - 5 * 60);
                Instant checkOut = endInstant.plusSeconds(random.nextInt(30 * 60) - 10 * 60);
                moveTo(em, assignment, AssignmentState.ACKNOWLEDGED, created.plusSeconds(3600));
                moveTo(em, assignment, AssignmentState.IN_PROGRESS, checkIn);
                moveTo(em, assignment, AssignmentState.COMPLETED, checkOut);
                assignment.setCheckInAt(checkIn);
                assignment.setCheckOutAt(checkOut);
            } else if (roll < 88) {
                moveTo(em, assignment, AssignmentState.AUTO_ACCEPTED, created.plusSeconds(86400));
                moveTo(em, assignment, AssignmentState.MISSED, endInstant);
            } else if (roll < 95) {
                moveTo(em, assignment, AssignmentState.CANCELLED, startInstant.minusSeconds(86400));
            } else {
                moveTo(em, assignment, AssignmentState.DECLINED, created.plusSeconds(7200));
            }
        } else if (!start.isAfter(now)) {
            Instant checkIn = startInstant.plusSeconds(random.nextInt(10 * 60));
            moveTo(em, assignment, AssignmentState.ACKNOWLEDGED, created.plusSeconds(3600));
            moveTo(em, assignment, AssignmentState.IN_PROGRESS, checkIn);
            assignment.setCheckInAt(checkIn);
        } else if (roll < 35) {
            moveTo(em, assignment, AssignmentState.ACKNOWLEDGED, created.plusSeconds(3600));
        } else if (roll < 45) {
            moveTo(em, assignment, AssignmentState.AUTO_ACCEPTED, created.plusSeconds(86400));
        } else if (roll < 50) {
            moveTo(em, assignment, AssignmentState.DECLINED, created.plusSeconds(7200));
        }
        // otherwise it stays PLANNED
    }

    private void moveTo(EntityManager em, Assignment assignment, AssignmentState next, Instant at) {
        AssignmentState previous = assignment.getState();
        if (!previous.canTransitionTo(next)) {
            throw new IllegalStateException("Populator tried an illegal transition " + previous + " -> " + next);
        }
        assignment.setState(next);
        addHistory(em, assignment, previous, next, at);
    }

    private void addHistory(EntityManager em, Assignment assignment, AssignmentState from, AssignmentState to, Instant at) {
        em.persist(new AssignmentStateHistory(assignment.getId(), from, to, SOURCE, at));
    }

    private Instant toInstant(LocalDateTime time) {
        return time.atZone(ZONE).toInstant();
    }

    private String randomPhone() {
        return String.valueOf(20_000_000 + random.nextInt(80_000_000));
    }

    private <T> T pick(T[] values) {
        return values[random.nextInt(values.length)];
    }

    private static String asciiLower(String value) {
        return value.toLowerCase(Locale.ROOT).replace("æ", "ae").replace("ø", "oe").replace("å", "aa");
    }
}

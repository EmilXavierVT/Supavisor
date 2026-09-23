package app.services.entityServices;

import app.dao.UserDAO;
import app.entities.User;
import app.exceptions.DuplicateUserException;
import app.exceptions.ValidationException;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class UserService {
    /** The system roles: what a user may do in the app. Company-specific roles (kitchen, cleaning ...) are custom roles. */
    public static final Set<String> SYSTEM_ROLES = Set.of("ADMIN", "USER");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_EMAIL_LENGTH = 254;
    // no 0/O, 1/l/I - the password is read off a screen and typed in by hand
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int TEMPORARY_PASSWORD_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** A freshly created user together with the one-time password to hand to them. */
    public record CreatedUser(User user, String temporaryPassword) {
    }

    private final UserDAO userDAO;

    public UserService(EntityManagerFactory emf) {
        this.userDAO = new UserDAO(emf);
    }

    public List<User> getAll() {
        return userDAO.getAll();
    }

    public User getById(Long id) {
        return userDAO.getById(id);
    }

    public User getByEmail(String email) {
        return userDAO.getByEmail(email);
    }

    public List<User> getByTenantId(Long tenantId) {
        return userDAO.getByTenantId(tenantId);
    }

    public User create(User user) {
        return userDAO.create(user);
    }

    /**
     * Creates an active user in the given tenant with one system role, any number of the
     * tenant's custom roles (null or empty for none) and a generated temporary password.
     * The email has to be well formed and not already in use.
     */
    public CreatedUser createUserWithRole(String name, String email, String role, Long tenantId,
                                          Set<Long> customRoleIds) throws ValidationException {
        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = email == null ? "" : email.trim();
        String cleanRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

        if (cleanName.isEmpty()) {
            throw new ValidationException("Name is required");
        }
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (cleanEmail.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ValidationException("A valid email is required");
        }
        if (!SYSTEM_ROLES.contains(cleanRole)) {
            throw new ValidationException("Role must be one of: " + String.join(", ", new TreeSet<>(SYSTEM_ROLES)));
        }
        if (tenantId == null) {
            throw new ValidationException("The administrator does not belong to a tenant");
        }
        if (userDAO.getByEmail(cleanEmail) != null) {
            throw new DuplicateUserException("A user with this email already exists");
        }

        String temporaryPassword = generateTemporaryPassword();
        User user = new User(null, cleanEmail, temporaryPassword, null, tenantId, true, Set.of(cleanRole));
        user.setName(cleanName);
        try {
            return new CreatedUser(userDAO.create(user, customRoleIds), temporaryPassword);
        } catch (PersistenceException e) {
            // lost a race against another request creating the same email
            if (userDAO.getByEmail(cleanEmail) != null) {
                throw new DuplicateUserException("A user with this email already exists");
            }
            throw e;
        }
    }

    private static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMPORARY_PASSWORD_LENGTH);
        for (int i = 0; i < TEMPORARY_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    public User update(User user) {
        return userDAO.update(user);
    }

    public User update(User user, Set<Long> customRoleIds) throws ValidationException {
        return userDAO.update(user, customRoleIds);
    }

    public User delete(Long id) {
        return userDAO.delete(id);
    }

    public User setCustomRoles(Long id, Set<Long> roleIds) throws ValidationException {
        return userDAO.setCustomRoles(id, roleIds);
    }

    public User addCustomRole(Long id, Long roleId) throws ValidationException {
        return userDAO.addCustomRole(id, roleId);
    }

    public User removeCustomRole(Long id, Long roleId) {
        return userDAO.removeCustomRole(id, roleId);
    }

    public User setPrimaryCategory(Long id, Long categoryId) throws ValidationException {
        return userDAO.setPrimaryCategory(id, categoryId);
    }

    public User clearPrimaryCategory(Long id) {
        return userDAO.clearPrimaryCategory(id);
    }

    public User setAdmin(Long id) {
        return userDAO.setRole(id, "ADMIN");
    }

    public User setEmployee(Long id) {
        return userDAO.setRole(id, "EMPLOYEE");
    }

    public User setCleaningStaff(Long id) {
        return userDAO.setRole(id, "CLEANING_STAFF");
    }

    public User setCleaningClient(Long id) {
        return userDAO.setRole(id, "CLEANING_CLIENT");
    }

    public User setSubscriber(Long id) {
        return userDAO.setRole(id, "SUBSCRIBER");
    }

    public User setFlex(Long id) {
        return userDAO.setRole(id, "FLEX");
    }

    public User reversActivation(long userID) {
        return userDAO.reversActivation(userID);
    }
}

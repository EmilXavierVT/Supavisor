package app.services.routeSecurity.routes;

import app.dto.RoleDTO;
import app.dto.UserDTO;
import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.DuplicateUserException;
import app.exceptions.ValidationException;
import app.services.dtoConverter.RoleMapper;
import app.services.dtoConverter.UserMapper;
import app.services.entityServices.UserService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UserRoutes {
    private final UserService userService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper = new RoleMapper();
    private static final Logger logger = LoggerFactory.getLogger(UserRoutes.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app.services.apiServices.routes");

    public UserRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.userService = new UserService(emf);
        this.userMapper = new UserMapper(emf);
    }

    public void getAll(Context ctx) {
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : userService.getAll()) {
            dtos.add(userMapper.toDto(user));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.getById(id);
        if (user == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.json(userMapper.toDto(user));
    }

    public void getByTenantId(Context ctx) {
        Long tenantId = ctx.pathParamAsClass("tenantId", Long.class).get();
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : userService.getByTenantId(tenantId)) {
            dtos.add(userMapper.toDto(user));
        }
        ctx.json(dtos);
    }

    public void create(Context ctx) {
        logger.info("Creating user");
        debugLogger.info("Creating user");
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        User user = userMapper.fromDto(dto);
        user.setIsActive(true);
        User created = userService.create(user);
        ctx.status(201).json(userMapper.toDto(created));
    }

    /**
     * Admin-only creation of a user with a name, a unique email, exactly one system role
     * (ADMIN or USER) and optionally some of the company's custom roles (customRoleIds).
     * The user always lands in the calling administrator's tenant, whatever the body says,
     * and custom roles from any other tenant are refused.
     */
    public void createUser(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null) {
            throw new ApiException(401, "Not authenticated");
        }
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        Set<String> roles = dto.getRoles();
        if (roles == null || roles.size() != 1) {
            throw new ApiException(400, "Exactly one system role (ADMIN or USER) is required");
        }

        try {
            UserService.CreatedUser created = userService.createUserWithRole(
                    dto.getName(), dto.getEmail(), roles.iterator().next(), caller.getTenantId(), dto.getCustomRoleIds());
            ctx.status(201).json(Map.of(
                    "user", userMapper.toDto(created.user()),
                    "temporaryPassword", created.temporaryPassword()));
        } catch (DuplicateUserException e) {
            throw new ApiException(409, e.getMessage());
        } catch (ValidationException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public void update(Context ctx) {
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        if (dto.getId() != null && dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            User existing = userService.getById(dto.getId());
            if (existing != null && isCaller(ctx, existing) && hasAdminRole(existing.getRoles())
                    && !hasAdminRole(dto.getRoles())) {
                throw new ApiException(403, SELF_DEMOTION_MESSAGE);
            }
        }
        User user = userMapper.fromDto(dto);
        User updated;
        try {
            updated = userService.update(user, dto.getCustomRoleIds());
        } catch (ValidationException e) {
            throw new ApiException(400, e.getMessage());
        }

        if (updated == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.json(userMapper.toDto(updated));
    }

    /**
     * Admin-only: replaces a user's company-defined (custom) roles - e.g. Kitchen, Cleaning - with the
     * given set. This is a full replace, not an addition: a role the user had before that is not in
     * customRoleIds (e.g. Kitchen, when the new set is just Cleaning) is removed.
     */
    public void updateUserRoles(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        UserDTO caller = ctx.attribute("user");
        if (caller == null) {
            throw new ApiException(401, "Not authenticated");
        }

        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        Set<Long> customRoleIds = dto.getCustomRoleIds();
        if (customRoleIds == null) {
            throw new ApiException(400, "customRoleIds is required");
        }

        User target = userService.getById(id);
        if (target == null || !Objects.equals(target.getTenantId(), caller.getTenantId())) {
            ctx.status(404).result("User not found");
            return;
        }

        try {
            respondWithUser(ctx, userService.setCustomRoles(id, customRoleIds));
        } catch (ValidationException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User target = userService.getById(id);
        if (target != null && isCaller(ctx, target)) {
            throw new ApiException(403, "You cannot delete your own account");
        }
        User deleted = userService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.status(204);
    }

    public void setAdmin(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setAdmin(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setEmployee(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        rejectSelfDemotion(ctx, id);
        User user = userService.setEmployee(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningStaff(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        rejectSelfDemotion(ctx, id);
        User user = userService.setCleaningStaff(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningClient(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        rejectSelfDemotion(ctx, id);
        User user = userService.setCleaningClient(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setSubscriber(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        rejectSelfDemotion(ctx, id);
        User user = userService.setSubscriber(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setFlex(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        rejectSelfDemotion(ctx, id);
        User user = userService.setFlex(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCustomRoles(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        if (dto.getCustomRoleIds() == null) {
            throw new ApiException(400, "customRoleIds is required");
        }
        try {
            respondWithUser(ctx, userService.setCustomRoles(id, dto.getCustomRoleIds()));
        } catch (ValidationException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public void addCustomRole(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Long roleId = ctx.pathParamAsClass("roleId", Long.class).get();
        try {
            respondWithUser(ctx, userService.addCustomRole(id, roleId));
        } catch (ValidationException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public void removeCustomRole(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Long roleId = ctx.pathParamAsClass("roleId", Long.class).get();
        respondWithUser(ctx, userService.removeCustomRole(id, roleId));
    }

    private static final String SELF_DEMOTION_MESSAGE =
            "You cannot remove administrative privileges from your own account";

    /** True when the authenticated caller is the given user. Prefers the id in the token, falls back to email for older tokens. */
    private boolean isCaller(Context ctx, User target) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || target == null) return false;
        if (caller.getId() != null) return caller.getId().equals(target.getId());
        return caller.getEmail() != null && caller.getEmail().equalsIgnoreCase(target.getEmail());
    }

    private static boolean hasAdminRole(Set<String> roles) {
        return roles != null && roles.stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }

    /** For the endpoints that replace a user's role: an admin may not swap their own ADMIN role for another one. */
    private void rejectSelfDemotion(Context ctx, Long targetId) {
        User target = userService.getById(targetId);
        if (target != null && isCaller(ctx, target) && hasAdminRole(target.getRoles())) {
            throw new ApiException(403, SELF_DEMOTION_MESSAGE);
        }
    }

    private void respondWithUser(Context ctx, User user) {
        if (user == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.json(userMapper.toDto(user));
    }

    public void reversActivation( Context ctx) {
        long employeeId = ctx.pathParamAsClass("id", Long.class).get();
        User target = userService.getById(employeeId);
        if (target != null && target.getIsActive() && isCaller(ctx, target)) {
            throw new ApiException(403, "You cannot deactivate your own account");
        }
        try{
        userService.reversActivation(employeeId);
            respondWithUser(ctx, userService.getById(employeeId));
            ctx.status(200);
            ctx.json("User deactivated successfully");
        } catch (Exception e) {
            ctx.status(404).result("User not found");
        }
    }
}

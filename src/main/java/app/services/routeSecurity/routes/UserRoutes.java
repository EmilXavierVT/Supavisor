package app.services.routeSecurity.routes;

import app.dto.UserDTO;
import app.entities.User;
import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import app.services.dtoConverter.UserMapper;
import app.services.entityServices.UserService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserRoutes {
    private final UserService userService;
    private final UserMapper userMapper;
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

    public void update(Context ctx) {
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
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

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
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
        User user = userService.setEmployee(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningStaff(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setCleaningStaff(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningClient(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setCleaningClient(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setSubscriber(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setSubscriber(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setFlex(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
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

    private void respondWithUser(Context ctx, User user) {
        if (user == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.json(userMapper.toDto(user));
    }

    public void deactivateEmployee( Context ctx) {
        long employeeId = ctx.pathParamAsClass("id", Long.class).get();
        try{
        userService.deactivateUser(employeeId);
            respondWithUser(ctx, userService.getById(employeeId));
            ctx.status(200);
            ctx.json("User deactivated successfully");
        } catch (Exception e) {
            ctx.status(404).result("User not found");
        }
    }
}

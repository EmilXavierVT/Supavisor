package app.controller;

import app.services.routeSecurity.ISecurityController;
import app.services.routeSecurity.ITokenSecurity;
import app.services.routeSecurity.TokenSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import app.exceptions.TokenVerificationException;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import app.config.HibernateConfig;
import jakarta.persistence.EntityManagerFactory;
import app.dao.ISecurityDAO;
import app.dao.UserDAO;
import app.entities.User;
import app.utils.Utils;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;

import java.text.ParseException;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController implements ISecurityController {
    private static final String REFRESHED_TOKEN_HEADER = "X-Refresh-Token";
    private static final String DEFAULT_TOKEN_REFRESH_GRACE_TIME = "1800000";
    private ISecurityDAO userDAO;

    public SecurityController() {
        this.userDAO = new UserDAO(HibernateConfig.getEntityManagerFactory());
    }

    public SecurityController(EntityManagerFactory emf) {
        this.userDAO = new UserDAO(emf);
    }
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ITokenSecurity tokenSecurity = new TokenSecurity();


    @Override
    public void register(Context ctx) {
        UserDTO user = ctx.bodyAsClass(UserDTO.class);
        try {
            User entity = userDAO.createUser(user.getEmail(), user.getPassword(), user.getPhoneNumber(), user.getTenantId());
            ObjectNode node = objectMapper.createObjectNode();
            node.put("msg", "register success")
                    .put("id",entity.getId());
            ctx.json(node).status(201);
        } catch (ValidationException e) {
            throw new ApiException(HttpStatus.CONFLICT.getCode(), e.getMessage());
        }
    }
    @Override
    public void login(Context ctx) {
        UserDTO user = ctx.bodyAsClass(UserDTO.class);
        try{
            User userEntity = userDAO.getVerifiedUser(user.getEmail(), user.getPassword());
            UserDTO tokenUser = new UserDTO();
            tokenUser.setRoles(userEntity.getRoles());
            tokenUser.setEmail(userEntity.getEmail());
            tokenUser.setTenantId(userEntity.getTenantId());
            tokenUser.setId(userEntity.getId());
            tokenUser.setIsActive(userEntity.getIsActive());
            String token = createToken(tokenUser);
            ObjectNode node =objectMapper.createObjectNode();

            String roles = String.join(",", userEntity.getRolesAsStrings());

            ctx.status(200).json(node
                    .put("token", token)
                    .put("username", userEntity.getEmail())
                    .put("accpted","indeed")
                    .put("id",userEntity.getId())
                    .put("role", roles)
                    .put("activity", userEntity.getIsActive()));




        }catch (ValidationException e){
            throw new ApiException(401,e.getMessage());
        }

    }




    @Override
    public void changePassword(Context ctx) {
        UserDTO tokenUser = ctx.attribute("user");
        if (tokenUser == null) {
            throw new ApiException(401, "Not authenticated");
        }
        ObjectNode body = ctx.bodyAsClass(ObjectNode.class);
        String currentPassword = body.path("currentPassword").asText(null);
        String newPassword = body.path("newPassword").asText(null);
        if (currentPassword == null || newPassword == null || newPassword.isBlank()) {
            throw new ApiException(400, "currentPassword and newPassword are required");
        }
        try {
            userDAO.changePassword(tokenUser.getEmail(), currentPassword, newPassword);
        } catch (ValidationException e) {
            throw new ApiException(400, e.getMessage());
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("msg", "Password changed successfully");
        ctx.json(node).status(200);
    }

    @Override
    public void sendVerifiedTokenResponse(Context ctx) {
        try {
            String token = getToken(ctx);
            verifyToken(token,ctx);
            ctx.status(200).json(objectMapper.createObjectNode()
                    .put("msg", "Token is valid"));
        } catch (ApiException e) {
            ctx.status(401).json(objectMapper.createObjectNode()
                    .put("msg", "Token is invalid"));

        }


    }

    private String createToken(UserDTO user) {
        try {
            String ISSUER = getConfigValue("ISSUER");
            String TOKEN_EXPIRE_TIME = getConfigValue("TOKEN_EXPIRE_TIME");
            String SECRET_KEY = getConfigValue("SECRET_KEY");
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch (Exception e) {
            throw new ApiException(500, "Could not create token");
        }
    }
    @Override
    public void authenticate(Context ctx) {
        if (ctx.method().toString().equals("OPTIONS")) {
            ctx.status(200);
            return;
        }
        Set<String> allowedRoles = ctx.routeRoles().stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
        if (isOpenEndpoint(allowedRoles))
            return;

        UserDTO verifiedTokenUser = validateAndGetUserFromToken(ctx);
        ctx.attribute("user", verifiedTokenUser);
    }

    @Override
    public void authorize(Context ctx) {
        Set<String> allowedRoles = ctx.routeRoles()
                .stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());

        if (isOpenEndpoint(allowedRoles))
            return;
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ForbiddenResponse("No user was added from the token");
        }
        if (!userHasAllowedRole(user, allowedRoles))
            throw new ForbiddenResponse("User was not authorized with roles: " + user.getRoles() + ". Needed roles are: " + allowedRoles);
    }


    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }


    private static String getToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null) {
            throw new UnauthorizedResponse("Authorization header is missing");
        }
        String[] parts = header.split(" ", 2);
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("Bearer") || parts[1].isBlank()) {
            throw new UnauthorizedResponse("Authorization header is malformed");
        }
        return parts[1];
    }



    private boolean isOpenEndpoint(Set<String> allowedRoles) {
        if (allowedRoles.isEmpty())
            return true;

        if (allowedRoles.contains("ANYONE")) {
            return true;
        }
        return false;
    }
    private UserDTO validateAndGetUserFromToken(Context ctx) {
        String token = getToken(ctx);
        UserDTO verifiedTokenUser = verifyToken(token, ctx);
        if (verifiedTokenUser == null) {
            throw new UnauthorizedResponse("Invalid user or token");
        }
        return verifiedTokenUser;
    }


    private UserDTO verifyToken(String token, Context ctx) {
        String SECRET = getConfigValue("SECRET_KEY");

        try {
            if (!tokenSecurity.tokenIsValid(token, SECRET)) {
                throw new ApiException(403, "Token is not valid");
            }

            UserDTO tokenUser = tokenSecurity.getUserWithRolesFromToken(token);
            UserDTO currentUser = loadCurrentUser(tokenUser);
            if (tokenSecurity.tokenNotExpired(token)) {
                return currentUser;
            }

            long refreshGraceTime = Long.parseLong(getOptionalConfigValue("TOKEN_REFRESH_GRACE_TIME", DEFAULT_TOKEN_REFRESH_GRACE_TIME));
            if (tokenSecurity.tokenExpiredWithin(token, refreshGraceTime)) {
                ctx.header(REFRESHED_TOKEN_HEADER, createToken(currentUser));
                return currentUser;
            }

            throw new ApiException(403, "Token is expired");
        } catch (ParseException | ApiException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        } catch (TokenVerificationException e) {
            throw new RuntimeException(e);
        }
    }

    private UserDTO loadCurrentUser(UserDTO tokenUser) {
        User user = null;
        if (tokenUser.getId() != null) {
            user = userDAO.getById(tokenUser.getId());
        }
        if (user == null && tokenUser.getEmail() != null) {
            user = userDAO.getByEmail(tokenUser.getEmail());
        }
        if (user == null || !user.getIsActive()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. User is not active");
        }

        UserDTO currentUser = new UserDTO(user.getEmail(), user.getRoles(), user.getIsActive());
        currentUser.setId(user.getId());
        currentUser.setTenantId(user.getTenantId());
        return currentUser;
    }

    private boolean isDeployed() {
        return System.getenv("DEPLOYED") != null || System.getenv("CONNECTION_STR") != null;
    }

    private String getConfigValue(String key) {
        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        return Utils.getPropertyValue(key, "config.properties");
    }

    private String getOptionalConfigValue(String key, String defaultValue) {
        try {
            String value = getConfigValue(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        } catch (RuntimeException e) {
            return defaultValue;
        }
        return defaultValue;
    }

    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new ApiException(500, key + " must be configured in the container environment");
        }
        return value.trim();
    }
}

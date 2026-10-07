package app.controller;

import app.dto.QualificationDTO;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.dtoConverter.UserMapper;
import app.services.entityServices.QualificationService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;

public class QualificationController {
    private final QualificationService qualificationService;
    private final UserMapper userMapper;

    public QualificationController(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.qualificationService = new QualificationService(emf);
        this.userMapper = new UserMapper(emf);
    }

    public void getAll(Context ctx) {
        ctx.json(qualificationService.getByTenant(callerTenantId(ctx)));
    }

    public void create(Context ctx) {
        QualificationDTO dto = ctx.bodyValidator(QualificationDTO.class).get();
        ctx.status(201).json(qualificationService.create(dto, callerTenantId(ctx)));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        QualificationDTO dto = ctx.bodyValidator(QualificationDTO.class).get();
        ctx.json(qualificationService.update(id, dto, callerTenantId(ctx)));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        qualificationService.delete(id, callerTenantId(ctx));
        ctx.status(204);
    }

    public void replaceUserQualifications(Context ctx) {
        Long userId = ctx.pathParamAsClass("id", Long.class).get();
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        Set<Long> qualificationIds = dto.getQualificationIds();
        if (qualificationIds == null) {
            throw new ApiException(400, "qualificationIds is required");
        }
        ctx.json(userMapper.toDto(qualificationService.replaceUserQualifications(userId, qualificationIds, callerTenantId(ctx))));
    }

    public void addUserQualification(Context ctx) {
        Long userId = ctx.pathParamAsClass("id", Long.class).get();
        Long qualificationId = ctx.pathParamAsClass("qualificationId", Long.class).get();
        ctx.json(userMapper.toDto(qualificationService.addUserQualification(userId, qualificationId, callerTenantId(ctx))));
    }

    public void removeUserQualification(Context ctx) {
        Long userId = ctx.pathParamAsClass("id", Long.class).get();
        Long qualificationId = ctx.pathParamAsClass("qualificationId", Long.class).get();
        ctx.json(userMapper.toDto(qualificationService.removeUserQualification(userId, qualificationId, callerTenantId(ctx))));
    }

    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }
}

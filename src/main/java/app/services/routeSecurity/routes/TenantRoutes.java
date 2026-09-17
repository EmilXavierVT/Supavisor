package app.services.routeSecurity.routes;

import app.dto.TenantDTO;
import app.entities.Tenant;
import app.services.dtoConverter.TenantMapper;
import app.services.entityServices.TenantService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TenantRoutes {
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;
    private static final Logger logger = LoggerFactory.getLogger(TenantRoutes.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app.services.apiServices.routes");

    public TenantRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.tenantService = new TenantService(emf);
        this.tenantMapper = new TenantMapper(emf);
    }

    public void getAll(Context ctx) {
        List<TenantDTO> dtos = new ArrayList<>();
        for (Tenant tenant : tenantService.getAll()) {
            dtos.add(tenantMapper.toDto(tenant));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            ctx.status(404).result("Tenant not found");
            return;
        }
        ctx.json(tenantMapper.toDto(tenant));
    }

    public void create(Context ctx) {
        logger.info("Creating tenant");
        debugLogger.info("Creating tenant");
        TenantDTO dto = ctx.bodyValidator(TenantDTO.class).get();
        Tenant tenant = tenantMapper.fromDto(dto);
        Tenant created = tenantService.create(tenant);
        ctx.status(201).json(tenantMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        TenantDTO dto = ctx.bodyValidator(TenantDTO.class).get();
        dto.setId(id);
        Tenant tenant = tenantMapper.fromDto(dto);
        Tenant updated = tenantService.update(tenant);
        if (updated == null) {
            ctx.status(404).result("Tenant not found");
            return;
        }
        ctx.json(tenantMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Tenant deleted = tenantService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("Tenant not found");
            return;
        }
        ctx.status(204);
    }
}

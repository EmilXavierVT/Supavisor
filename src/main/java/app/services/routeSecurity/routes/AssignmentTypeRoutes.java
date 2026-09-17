package app.services.routeSecurity.routes;

import app.dto.AssignmentTypeDTO;
import app.services.entityServices.AssignmentTypeService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class AssignmentTypeRoutes {

    private final AssignmentTypeService assignmentTypeService;

    public AssignmentTypeRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.assignmentTypeService = new AssignmentTypeService(emf);
    }

    public void getAll(Context ctx) {
        boolean activeOnly = Boolean.parseBoolean(ctx.queryParam("activeOnly"));
        List<AssignmentTypeDTO> dtos = assignmentTypeService.getAllAssignmentTypes(activeOnly);
        ctx.json(dtos);
    }

    public void create(Context ctx) {
        AssignmentTypeDTO dto = ctx.bodyValidator(AssignmentTypeDTO.class).get();
        AssignmentTypeDTO created = assignmentTypeService.createAssignmentType(dto);
        ctx.status(201).json(created);
    }

    public void update(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();
        AssignmentTypeDTO dto = ctx.bodyValidator(AssignmentTypeDTO.class).get();
        AssignmentTypeDTO updated = assignmentTypeService.updateAssignmentType(id, dto);
        if (updated == null) {
            ctx.status(404).result("AssignmentType not found");
            return;
        }
        ctx.json(updated);
    }

    public void delete(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();
        assignmentTypeService.deleteAssignmentType(id);
        ctx.status(204);
    }

    public void deactivate(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();
        assignmentTypeService.deactivateAssignmentType(id);
        ctx.status(200);
    }
}
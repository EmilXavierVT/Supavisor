package app.controller;

import app.dto.AssignmentTypeDTO;
import app.services.entityServices.AssignmentTypeService;
import io.javalin.http.Context;

public class AssignmentTypeController {

    private final AssignmentTypeService service;

    public AssignmentTypeController(AssignmentTypeService service) {
        this.service = service;
    }

    public void getAll(Context ctx) {
        boolean activeOnly = Boolean.parseBoolean(ctx.queryParam("activeOnly"));
        ctx.json(service.getAllAssignmentTypes(activeOnly));
    }

    public void create(Context ctx) {
        var req = ctx.bodyAsClass(AssignmentTypeDTO.class);
        ctx.status(201).json(service.createAssignmentType(req));
    }

    public void update(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();
        var req = ctx.bodyAsClass(AssignmentTypeDTO.class);
        ctx.json(service.updateAssignmentType(id, req));
    }

    public void delete(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();
        service.deleteAssignmentType(id);
        ctx.status(204);
    }

    public void deactivate(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();
        service.deactivateAssignmentType(id);
        ctx.status(200);
    }
}
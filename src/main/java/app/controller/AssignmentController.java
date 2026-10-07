package app.controller;



import app.dto.AssignmentDTO;
import app.dto.AssignmentDelegationRequestDTO;
import app.dto.AssignmentStateUpdateDTO;
import app.dto.AttendanceCorrectionDTO;
import app.dto.UserDTO;
import app.exceptions.ApiException;
import app.services.entityServices.AssignmentDelegationService;
import app.services.entityServices.AssignmentService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class AssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentDelegationService delegationService;

    public AssignmentController(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.assignmentService = new AssignmentService(emf);
        this.delegationService = new AssignmentDelegationService(emf);
    }

    public void getAll(Context ctx) {
        boolean admin = isAdmin(ctx);
        boolean activeOnly = !admin || Boolean.parseBoolean(ctx.queryParam("activeOnly"));
        if (admin) {
            ctx.json(assignmentService.getAll(callerTenantId(ctx), activeOnly));
        } else {
            ctx.json(assignmentService.getVisibleToUser(callerTenantId(ctx), callerId(ctx), activeOnly));
        }
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        if (isAdmin(ctx)) {
            ctx.json(assignmentService.getById(id, callerTenantId(ctx), false));
        } else {
            ctx.json(assignmentService.getVisibleById(id, callerTenantId(ctx), callerId(ctx)));
        }
    }

    public void create(Context ctx) {
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        ctx.status(201).json(assignmentService.create(dto, callerTenantId(ctx), callerId(ctx), callerSource(ctx)));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        ctx.json(assignmentService.update(id, dto, callerTenantId(ctx), callerId(ctx), callerSource(ctx)));
    }

    public void previewOverlaps(Context ctx) {
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        Long ownId = ctx.queryParam("assignmentId") == null
                ? null
                : Long.valueOf(ctx.queryParam("assignmentId"));
        ctx.json(assignmentService.previewOverlaps(dto, callerTenantId(ctx), ownId));
    }

    public void setResponsible(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentDTO dto = ctx.bodyValidator(AssignmentDTO.class).get();
        ctx.json(assignmentService.setResponsible(id, dto.getAssignedEmployeeId(), callerTenantId(ctx), callerSource(ctx)));
    }

    public void clearResponsible(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.clearResponsible(id, callerTenantId(ctx), callerSource(ctx)));
    }

    public void getDelegations(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(delegationService.getDelegations(id, callerTenantId(ctx)));
    }

    public void delegate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentDelegationRequestDTO dto = ctx.bodyValidator(AssignmentDelegationRequestDTO.class).get();
        delegationService.delegate(id, dto.getEmployeeId(), callerTenantId(ctx), callerId(ctx), callerSource(ctx));
        ctx.status(204);
    }

    public void undelegate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Long employeeId = ctx.pathParamAsClass("employeeId", Long.class).get();
        delegationService.undelegate(id, employeeId, callerTenantId(ctx), callerSource(ctx));
        ctx.status(204);
    }

    public void deactivate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.deactivate(id, callerTenantId(ctx), callerSource(ctx)));
    }

    public void activate(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.activate(id, callerTenantId(ctx), callerSource(ctx)));
    }

    public void changeState(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AssignmentStateUpdateDTO dto = ctx.bodyValidator(AssignmentStateUpdateDTO.class).get();
        ctx.json(assignmentService.changeState(id, dto.getState(), callerTenantId(ctx), callerSource(ctx)));
    }

    public void checkIn(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.checkIn(id, callerTenantId(ctx), callerId(ctx), isAdmin(ctx), callerSource(ctx)));
    }

    public void checkOut(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.checkOut(id, callerTenantId(ctx), callerId(ctx), isAdmin(ctx), callerSource(ctx)));
    }

    public void getStateHistory(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.getStateHistory(id, callerTenantId(ctx)));
    }

    public void getHistory(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.getHistory(id, callerTenantId(ctx)));
    }

    public void correctAttendance(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        AttendanceCorrectionDTO dto = ctx.bodyValidator(AttendanceCorrectionDTO.class).get();
        ctx.json(assignmentService.correctAttendance(id, callerTenantId(ctx), callerId(ctx), callerSource(ctx), dto));
    }

    public void getAttendanceHistory(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ctx.json(assignmentService.getAttendanceHistory(id, callerTenantId(ctx), callerId(ctx), isAdmin(ctx)));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        assignmentService.delete(id, callerTenantId(ctx), callerSource(ctx));
        ctx.status(204);
    }

    // the tenant comes from the token, never from the request
    private static Long callerTenantId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getTenantId() == null) {
            throw new ApiException(401, "Not authenticated or tenantId missing from token");
        }
        return caller.getTenantId();
    }

    private static Long callerId(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getId() == null) {
            throw new ApiException(401, "Not authenticated or userId missing from token");
        }
        return caller.getId();
    }

    private static boolean isAdmin(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        return caller != null && caller.getRoles() != null
                && caller.getRoles().stream().anyMatch("ADMIN"::equalsIgnoreCase);
    }

    private static String callerSource(Context ctx) {
        UserDTO caller = ctx.attribute("user");
        if (caller == null || caller.getEmail() == null || caller.getEmail().isBlank()) {
            return "api";
        }
        return caller.getEmail();
    }
}

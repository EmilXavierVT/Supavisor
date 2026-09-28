package app.services.entityServices;

import app.dao.AssignmentDAO;
import app.dao.ProjectDAO;
import app.dto.ProjectDTO;
import app.dto.ProjectStatusHistoryDTO;
import app.entities.Assignment;
import app.entities.Project;
import app.entities.ProjectStatus;
import app.entities.ProjectStatusHistory;
import app.exceptions.ApiException;
import app.services.dtoConverter.ProjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;

public class ProjectService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private final ProjectDAO dao;
    private final AssignmentDAO assignmentDAO;
    private final ProjectMapper mapper = new ProjectMapper();

    public ProjectService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.dao = new ProjectDAO(emf);
        this.assignmentDAO = new AssignmentDAO(emf);
    }

    public List<ProjectDTO> getAll(Long tenantId) {
        return dao.getAll(tenantId).stream()
                .map(mapper::toDto)
                .toList();
    }

    public ProjectDTO getById(Long id, Long tenantId) {
        return mapper.toDto(find(id, tenantId));
    }

    public ProjectDTO create(ProjectDTO dto, Long tenantId, String actor) {
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, null);

        Instant now = Instant.now();
        Project project = new Project();
        project.setTenantId(tenantId);
        project.setName(name);
        applyDetails(project, dto, tenantId);
        project.setCreatedBy(actor);
        project.setCreatedAt(now);
        project.setUpdatedBy(actor);
        project.setUpdatedAt(now);

        ProjectStatusHistory history = new ProjectStatusHistory(null, null, project.getStatus(), actor, now);

        try {
            return mapper.toDto(dao.create(project, history));
        } catch (PersistenceException e) {
            rejectDuplicate(tenantId, name, null);
            throw e;
        }
    }

    public ProjectDTO update(Long id, ProjectDTO dto, Long tenantId, String actor) {
        Project existing = find(id, tenantId);
        ProjectStatus previousStatus = existing.getStatus();
        String name = validName(dto.getName());
        rejectDuplicate(tenantId, name, id);

        Instant now = Instant.now();
        existing.setName(name);
        applyDetails(existing, dto, tenantId);
        existing.setUpdatedBy(actor);
        existing.setUpdatedAt(now);

        ProjectStatusHistory history = null;
        if (previousStatus != existing.getStatus()) {
            history = new ProjectStatusHistory(existing.getId(), previousStatus, existing.getStatus(), actor, now);
        }

        try {
            return mapper.toDto(dao.update(existing, history));
        } catch (PersistenceException e) {
            rejectDuplicate(tenantId, name, id);
            throw e;
        }
    }

    public List<ProjectStatusHistoryDTO> getStatusHistory(Long id, Long tenantId) {
        Project project = find(id, tenantId);
        return dao.getStatusHistory(project.getId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    public void delete(Long id, Long tenantId) {
        find(id, tenantId);
        dao.delete(id);
    }

    private Project find(Long id, Long tenantId) {
        Project project = id == null ? null : dao.getById(id);
        if (project == null || !project.getTenantId().equals(tenantId)) {
            throw new ApiException(404, "Project not found");
        }
        return project;
    }

    private void applyDetails(Project project, ProjectDTO dto, Long tenantId) {
        project.setDescription(validDescription(dto.getDescription()));
        project.setAssignmentIds(validAssignmentIds(dto.getAssignmentIds(), tenantId));
        project.setStatus(dto.getStatus() == null ? ProjectStatus.DRAFT : dto.getStatus());
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(400, "Project name is required");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(400, "Project name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Project name contains invalid characters");
        }
        return trimmed;
    }

    private static String validDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ApiException(400, "Project description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Project description contains invalid characters");
        }
        return trimmed;
    }

    private List<Long> validAssignmentIds(List<Long> assignmentIds, Long tenantId) {
        if (assignmentIds == null || assignmentIds.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(assignmentIds);
        if (uniqueIds.contains(null)) {
            throw new ApiException(400, "Assignment id is required");
        }

        for (Long assignmentId : uniqueIds) {
            Assignment assignment = assignmentDAO.getById(assignmentId);
            if (assignment == null || !tenantId.equals(assignment.getTenantId())) {
                throw new ApiException(400, "Assignment not found, it may not belong to you");
            }
        }
        return List.copyOf(uniqueIds);
    }

    private void rejectDuplicate(Long tenantId, String name, Long ownId) {
        boolean taken = dao.findByName(tenantId, name).stream()
                .anyMatch(other -> !other.getId().equals(ownId));
        if (taken) {
            throw new ApiException(409, "A project with this name already exists");
        }
    }
}

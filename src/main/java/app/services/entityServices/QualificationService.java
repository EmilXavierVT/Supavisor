package app.services.entityServices;

import app.dao.QualificationDAO;
import app.dao.UserDAO;
import app.dto.QualificationDTO;
import app.entities.Qualification;
import app.entities.Tenant;
import app.entities.User;
import app.exceptions.ApiException;
import app.services.dtoConverter.QualificationMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Set;

public class QualificationService {
    private static final int MAX_NAME_LENGTH = 255;

    private final QualificationDAO qualificationDAO;
    private final UserDAO userDAO;
    private final QualificationMapper mapper = new QualificationMapper();
    private final EntityManagerFactory emf;

    public QualificationService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.qualificationDAO = new QualificationDAO(emf);
        this.userDAO = new UserDAO(emf);
    }

    public List<QualificationDTO> getByTenant(Long tenantId) {
        return qualificationDAO.findByTenantId(tenantId).stream()
                .map(mapper::toDTO)
                .toList();
    }

    public QualificationDTO create(QualificationDTO dto, Long tenantId) {
        String name = validName(dto == null ? null : dto.getName());
        requireTenant(tenantId);
        rejectDuplicate(tenantId, name, null);
        Qualification saved = qualificationDAO.save(Qualification.builder()
                .tenantId(tenantId)
                .name(name)
                .build());
        return mapper.toDTO(saved);
    }

    public QualificationDTO update(Long id, QualificationDTO dto, Long tenantId) {
        Qualification existing = find(id, tenantId);
        String name = validName(dto == null ? null : dto.getName());
        rejectDuplicate(tenantId, name, id);
        existing.setName(name);
        return mapper.toDTO(qualificationDAO.save(existing));
    }

    public void delete(Long id, Long tenantId) {
        find(id, tenantId);
        qualificationDAO.delete(id);
    }

    public User replaceUserQualifications(Long userId, Set<Long> qualificationIds, Long tenantId) {
        User user = findUser(userId, tenantId);
        try {
            return qualificationDAO.replaceUserQualifications(user.getId(), qualificationIds);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public User addUserQualification(Long userId, Long qualificationId, Long tenantId) {
        User user = findUser(userId, tenantId);
        find(qualificationId, tenantId);
        boolean alreadyAssigned = user.getQualifications().stream()
                .anyMatch(qualification -> qualification.getId().equals(qualificationId));
        if (alreadyAssigned) {
            throw new ApiException(409, "Qualification already assigned to user");
        }
        try {
            return qualificationDAO.addUserQualification(user.getId(), qualificationId);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    public User removeUserQualification(Long userId, Long qualificationId, Long tenantId) {
        findUser(userId, tenantId);
        return qualificationDAO.removeUserQualification(userId, qualificationId);
    }

    public Set<Long> currentQualificationIds(Long userId, Long tenantId) {
        return findUser(userId, tenantId).getQualifications().stream()
                .map(Qualification::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private Qualification find(Long id, Long tenantId) {
        if (id == null) {
            throw new ApiException(400, "Qualification id is required");
        }
        Qualification qualification = qualificationDAO.findById(id);
        if (qualification == null || !tenantId.equals(qualification.getTenantId())) {
            throw new ApiException(404, "Qualification not found");
        }
        return qualification;
    }

    private User findUser(Long userId, Long tenantId) {
        if (userId == null) {
            throw new ApiException(400, "User id is required");
        }
        User user = userDAO.getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            throw new ApiException(404, "User not found");
        }
        return user;
    }

    private void rejectDuplicate(Long tenantId, String name, Long ownId) {
        Qualification duplicate = qualificationDAO.findByName(tenantId, name);
        if (duplicate != null && !duplicate.getId().equals(ownId)) {
            throw new ApiException(409, "A qualification with this name already exists");
        }
    }

    private void requireTenant(Long tenantId) {
        try (EntityManager em = emf.createEntityManager()) {
            if (tenantId == null || em.find(Tenant.class, tenantId) == null) {
                throw new ApiException(400, "Your company (tenant " + tenantId + ") does not exist");
            }
        }
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(400, "Qualification name is required");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(400, "Qualification name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ApiException(400, "Qualification name contains invalid characters");
        }
        return trimmed;
    }
}

package app.services.entityServices;

import app.dao.UserDAO;
import app.entities.User;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class UserService {
    private final UserDAO userDAO;

    public UserService(EntityManagerFactory emf) {
        this.userDAO = new UserDAO(emf);
    }

    public List<User> getAll() {
        return userDAO.getAll();
    }

    public User getById(Long id) {
        return userDAO.getById(id);
    }

    public User getByEmail(String email) {
        return userDAO.getByEmail(email);
    }

    public List<User> getByTenantId(Long tenantId) {
        return userDAO.getByTenantId(tenantId);
    }

    public User create(User user) {
        return userDAO.create(user);
    }

    public User update(User user) {
        return userDAO.update(user);
    }

    public User delete(Long id) {
        return userDAO.delete(id);
    }

    public User setAdmin(Long id) {
        return userDAO.setRole(id, "ADMIN");
    }

    public User setEmployee(Long id) {
        return userDAO.setRole(id, "EMPLOYEE");
    }

    public User setCleaningStaff(Long id) {
        return userDAO.setRole(id, "CLEANING_STAFF");
    }

    public User setCleaningClient(Long id) {
        return userDAO.setRole(id, "CLEANING_CLIENT");
    }

    public User setSubscriber(Long id) {
        return userDAO.setRole(id, "SUBSCRIBER");
    }

    public User setFlex(Long id) {
        return userDAO.setRole(id, "FLEX");
    }
}

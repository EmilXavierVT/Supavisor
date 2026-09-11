package app.dao;

import app.entities.User;
import app.exceptions.ValidationException;

public interface ISecurityDAO {
    User createUser(String email, String password) throws ValidationException;

    User getVerifiedUser(String email, String password) throws ValidationException;

    void changePassword(String email, String currentPassword, String newPassword) throws ValidationException;
}

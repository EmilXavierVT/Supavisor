package app.services.dtoConverter;

import app.dto.UserDTO;
import app.entities.User;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashSet;

public class UserMapper {
    public UserMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
    }

    public UserDTO toDto(User user) {
        if (user == null) return null;
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                null,
                user.getPhoneNumber(),
                user.getTenantId(),
                new HashSet<>(user.getRoles())
        );
    }

    public User fromDto(UserDTO dto) {
        if (dto == null) return null;
        return new User(
                dto.getId(),
                dto.getEmail(),
                dto.getPassword(),
                dto.getPhoneNumber(),
                dto.getTenantId(),
                dto.getRoles() == null ? new HashSet<>() : new HashSet<>(dto.getRoles())
        );
    }
}

package app.services.dtoConverter;

import app.dto.UserDTO;
import app.entities.User;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashSet;
import java.util.stream.Collectors;

public class UserMapper {
    public UserMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
    }

    private final RoleMapper roleMapper = new RoleMapper();

    public UserDTO toDto(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO(
                user.getId(),
                user.getEmail(),
                null,
                user.getPhoneNumber(),
                user.getTenantId(),
                new HashSet<>(user.getRoles())
        );
        dto.setCustomRoles(user.getCustomRoles().stream()
                .map(roleMapper::toDTO)
                .collect(Collectors.toSet()));
        return dto;
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

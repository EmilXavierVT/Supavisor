package app.dto;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDTOTest {

    @Test
    void defaultConstructorStartsWithEmptyRoles() {
        UserDTO dto = new UserDTO();

        assertTrue(dto.getRoles().isEmpty());
    }

    @Test
    void constructorCopiesRolesDefensively() {
        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");

        UserDTO dto = new UserDTO("admin@example.com", roles);
        roles.add("USER");

        assertEquals(Set.of("ADMIN"), dto.getRoles());
        assertNotSame(roles, dto.getRoles());
    }
}

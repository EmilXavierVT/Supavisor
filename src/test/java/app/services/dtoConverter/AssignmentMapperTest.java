package app.services.dtoConverter;

import app.dto.AssignmentDTO;
import app.entities.Assignment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentMapperTest {

    @Test
    void responseContainsTheVersionNeededForTheNextUpdate() {
        Assignment assignment = new Assignment();
        assignment.setId(42L);
        assignment.setVersion(7L);
        assignment.setName("Kitchen prep");
        assignment.setTenantId(9L);

        AssignmentDTO dto = new AssignmentMapper().toDto(assignment);

        assertEquals(7L, dto.getVersion());
    }
}

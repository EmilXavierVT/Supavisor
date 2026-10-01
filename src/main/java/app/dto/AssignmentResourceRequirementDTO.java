package app.dto;

import app.entities.AssignmentResourceMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResourceRequirementDTO {
    private Long productId;
    private AssignmentResourceMode mode;
}

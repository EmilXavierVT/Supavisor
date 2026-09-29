package app.dto;

import app.entities.AssignmentState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignmentStateUpdateDTO {
    private AssignmentState state;
}

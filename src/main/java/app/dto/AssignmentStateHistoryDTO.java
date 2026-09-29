package app.dto;

import app.entities.AssignmentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentStateHistoryDTO {

    private Long id;
    private Long assignmentId;
    private AssignmentState fromState;
    private AssignmentState toState;
    private String source;
    private Instant changedAt;
}

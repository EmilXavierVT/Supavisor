package app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentOverlapDTO {
    private Long assignmentId;
    private String assignmentName;
    private Long assignedEmployeeId;
    private LocalDateTime startTime;
    private LocalDateTime estimatedEndTime;
}

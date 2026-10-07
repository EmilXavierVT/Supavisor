package app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDelegationDTO {
    private Long assignmentId;
    private Long employeeId;
    private String employeeName;
    private String employeeEmail;
    private Long delegatedByUserId;
    private String delegatedBy;
    private Instant delegatedAt;
}

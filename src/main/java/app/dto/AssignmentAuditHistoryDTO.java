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
public class AssignmentAuditHistoryDTO {
    private Long id;
    private Long assignmentId;
    private String auditType;
    private Long actorUserId;
    private String actorSource;
    private String reason;
    private String details;
    private Instant createdAt;
}

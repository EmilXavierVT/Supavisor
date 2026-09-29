package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "assignment_audit_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentAuditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "audit_type", nullable = false)
    private String auditType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_source", nullable = false)
    private String actorSource;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false, length = 4000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

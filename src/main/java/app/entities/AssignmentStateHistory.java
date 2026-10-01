package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "assignment_state_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state")
    private AssignmentState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false)
    private AssignmentState toState;

    @Column(nullable = false)
    private String source;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public AssignmentStateHistory(Long assignmentId, AssignmentState fromState, AssignmentState toState, String source, Instant changedAt) {
        this.assignmentId = assignmentId;
        this.fromState = fromState;
        this.toState = toState;
        this.source = source;
        this.changedAt = changedAt;
    }
}

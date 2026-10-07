package app.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "assignment_delegations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDelegation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    // no foreign key, so the record survives if the administrator account is removed later
    @Column(name = "delegated_by_user_id", nullable = false)
    private Long delegatedByUserId;

    @Column(name = "delegated_by", nullable = false)
    private String delegatedBy;

    @Column(name = "delegated_at", nullable = false)
    private Instant delegatedAt;
}

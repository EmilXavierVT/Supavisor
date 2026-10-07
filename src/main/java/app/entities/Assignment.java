package app.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "is_active", nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private boolean isFlagged = false;

    @Column(name = "missing_employee_count", nullable = false)
    @Builder.Default
    private int missingEmployeeCount = 0;

    private String address;

    @Column(length = 1000)
    private String notes;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PLANNED'")
    @Builder.Default
    private AssignmentState state = AssignmentState.PLANNED;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private User assignedEmployee;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assignment_products", joinColumns = @JoinColumn(name = "assignment_id"))
    @OrderColumn(name = "product_order")
    @Column(name = "product_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private List<Long> productIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assignment_resource_requirements", joinColumns = @JoinColumn(name = "assignment_id"))
    @OrderColumn(name = "resource_order")
    @Setter(AccessLevel.NONE)
    private List<AssignmentResourceRequirement> resourceRequirements = new ArrayList<>();

    public Assignment(Long id, String name, Long tenantId, boolean isActive) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.isActive = isActive;
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Long getAssignedEmployeeId() {
        return assignedEmployee == null ? null : assignedEmployee.getId();
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds == null ? new ArrayList<>() : new ArrayList<>(productIds);
    }

    public void setResourceRequirements(List<AssignmentResourceRequirement> resourceRequirements) {
        this.resourceRequirements = resourceRequirements == null ? new ArrayList<>() : new ArrayList<>(resourceRequirements);
    }
}

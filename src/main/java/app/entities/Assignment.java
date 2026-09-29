package app.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A template for a recurring piece of work, e.g. "Cleaning at Main Street 1", that an administrator
 * defines for their company: where it is, how long it should take, what it costs and who it is linked to.
 * Names are unique per tenant. Deactivating keeps the row so anything that references it keeps working.
 */
@Entity
@Table(name = "assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    private String address;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    // the employee (user) this assignment is linked to; cleared when that user is deleted
    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assignment_products", joinColumns = @JoinColumn(name = "assignment_id"))
    @OrderColumn(name = "product_order")
    @Column(name = "product_id", nullable = false)
    private List<Long> productIds = new ArrayList<>();

    public Assignment() {
        this.isActive = true;
    }

    public Assignment(Long id, String name, Long tenantId, boolean isActive) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEstimatedEndTime() { return estimatedEndTime; }
    public void setEstimatedEndTime(LocalDateTime estimatedEndTime) { this.estimatedEndTime = estimatedEndTime; }

    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public void setAssignedEmployeeId(Long assignedEmployeeId) { this.assignedEmployeeId = assignedEmployeeId; }

    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds == null ? new ArrayList<>() : new ArrayList<>(productIds);
    }
}

package app.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;

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

    @Column(name = "is_flagged", nullable = false)
    private boolean isFlagged;

    private String address;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    // the employee (user) this assignment is linked to; cleared when that user is deleted
    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    // Counter that tracks removed employees waiting for replacement
    @Column(name = "missing_employee_count", nullable = false)
    private int missingEmployeeCount = 0;

    public Assignment() {
        this.isActive = true;
        this.isFlagged = false;
        this.missingEmployeeCount = 0;
    }

    public Assignment(Long id, String name, Long tenantId, boolean isActive) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.isActive = isActive;
        this.isFlagged = false;
        this.missingEmployeeCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public void setAssignedEmployeeId(Long assignedEmployeeId) { this.assignedEmployeeId = assignedEmployeeId; }

    public int getMissingEmployeeCount() { return missingEmployeeCount; }
    public void setMissingEmployeeCount(int missingEmployeeCount) { this.missingEmployeeCount = missingEmployeeCount; }
}
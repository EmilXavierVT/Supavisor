package app.dto;

import java.math.BigDecimal;

public class AssignmentDTO {

    private Long id;
    private String name;
    private Long tenantId;
    private Boolean isActive;
    private String address;
    private Integer estimatedMinutes;
    private BigDecimal cost;
    private Long assignedEmployeeId;

    public AssignmentDTO() {}

    public AssignmentDTO(Long id, String name, Long tenantId, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // output only: the tenant always comes from the caller's token, never from the request body
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public void setAssignedEmployeeId(Long assignedEmployeeId) { this.assignedEmployeeId = assignedEmployeeId; }
}

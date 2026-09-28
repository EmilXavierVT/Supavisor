package app.dto;

import app.entities.Assignment;
import java.math.BigDecimal;

public class AssignmentDTO {

    private Long id;
    private String name;
    private Long tenantId;
    private Boolean isActive;
    private Boolean isFlagged;
    private String address;
    private Integer estimatedMinutes;
    private BigDecimal cost;
    private Long assignedEmployeeId;
    private Integer missingEmployeeCount;

    public AssignmentDTO() {}

    public AssignmentDTO(Long id, String name, Long tenantId, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.isActive = isActive;
    }


    public AssignmentDTO(Assignment entity) {
        if (entity != null) {
            this.id = entity.getId();
            this.name = entity.getName();
            this.tenantId = entity.getTenantId();
            this.isActive = entity.isActive(); // or entity.getIsActive() depending on your entity
            this.isFlagged = entity.isFlagged();
            this.address = entity.getAddress();
            this.estimatedMinutes = entity.getEstimatedMinutes();
            this.cost = entity.getCost();
            this.assignedEmployeeId = entity.getAssignedEmployeeId();
            this.missingEmployeeCount = entity.getMissingEmployeeCount();
        }
    }

    public Assignment toEntity() {
        Assignment entity = new Assignment();
        entity.setId(this.id);
        entity.setName(this.name);
        entity.setTenantId(this.tenantId);
        entity.setActive(this.isActive != null ? this.isActive : true);
        entity.setFlagged(this.isFlagged != null ? this.isFlagged : false);
        entity.setAddress(this.address);
        entity.setEstimatedMinutes(this.estimatedMinutes);
        entity.setCost(this.cost);
        entity.setAssignedEmployeeId(this.assignedEmployeeId);
        return entity;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public Boolean getIsFlagged() { return isFlagged; }
    public void setIsFlagged(Boolean flagged) { isFlagged = flagged; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public void setAssignedEmployeeId(Long assignedEmployeeId) { this.assignedEmployeeId = assignedEmployeeId; }

    public Integer getMissingEmployeeCount() { return missingEmployeeCount; }
    public void setMissingEmployeeCount(Integer missingEmployeeCount) { this.missingEmployeeCount = missingEmployeeCount; }
}
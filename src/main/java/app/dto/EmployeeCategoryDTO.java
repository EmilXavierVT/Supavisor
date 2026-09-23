package app.dto;

public class EmployeeCategoryDTO {
    private Long id;
    private String name;
    private Long tenantId;
    private Boolean active;

    public EmployeeCategoryDTO() {
    }

    public EmployeeCategoryDTO(Long id, String name, Long tenantId, Boolean active) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

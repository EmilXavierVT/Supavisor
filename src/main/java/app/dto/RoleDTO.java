package app.dto;

public class RoleDTO {
    private Long id;
    private String roleName;
    private Long tenantId;

    public RoleDTO() {
    }

    public RoleDTO(Long id, String roleName, Long tenantId) {
        this.id = id;
        this.roleName = roleName;
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}

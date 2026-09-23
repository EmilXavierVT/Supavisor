package app.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
public class EmployeeCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public EmployeeCategory() {
    }

    public EmployeeCategory(Long id, String name, Long tenantId, boolean active) {
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

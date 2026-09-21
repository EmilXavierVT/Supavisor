package app.entities;

import jakarta.persistence.*;

/**
 * A recurring kind of work (cleaning, kitchen ...) that an administrator defines for their company.
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
}

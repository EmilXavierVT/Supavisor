package app.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "role_name"}))
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    public Role() {
    }

    public Role(Long id, String roleName, Tenant tenant) {
        this.id = id;
        this.roleName = roleName;
        this.tenant = tenant;
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

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String roleName;
        private Tenant tenant;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        public Builder tenant(Tenant tenant) {
            this.tenant = tenant;
            return this;
        }

        public Role build() {
            return new Role(id, roleName, tenant);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return id != null && Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

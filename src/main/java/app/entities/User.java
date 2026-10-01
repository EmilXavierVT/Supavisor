package app.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(nullable = false)
    private String password;

    private String phoneNumber;

    @Column(name = "primary_category")
    private String primaryCategory;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Setter(AccessLevel.NONE)
    private Set<String> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_custom_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Setter(AccessLevel.NONE)
    private Set<Role> customRoles = new HashSet<>();

    @Column(name = "is_active")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isActive;

    public User(Long id, String email, String password, String phoneNumber, Long tenantId, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.tenantId = tenantId;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public User(Long id, String email, String password, String phoneNumber, Long tenantId, boolean isActive, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.tenantId = tenantId;
        this.isActive = isActive;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public User(Long id, String email, String password, String phoneNumber, Long tenantId, Set<String> roles, Set<Role> customRoles) {
        this(id, email, password, phoneNumber, tenantId, roles);
        this.customRoles = customRoles == null ? new HashSet<>() : new HashSet<>(customRoles);
    }

    public User(Long id, String email, String password, String phoneNumber, Set<String> roles) {
        this(id, email, password, phoneNumber, null, roles);
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public void setCustomRoles(Set<Role> customRoles) {
        this.customRoles = customRoles == null ? new HashSet<>() : new HashSet<>(customRoles);
    }

    public void addCustomRole(Role role) {
        customRoles.add(role);
    }

    public void removeCustomRole(Role role) {
        customRoles.remove(role);
    }

    public Set<String> getRolesAsStrings() {
        return roles;
    }

    public void replaceRole(String role) {
        roles.clear();
        roles.add(role);
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void reversActivation() {
        if (isActive) isActive = false;
        else if (!isActive) isActive = true; {
        }
    }
}

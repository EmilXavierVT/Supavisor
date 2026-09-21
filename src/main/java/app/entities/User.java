package app.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phoneNumber;

    @Column(name = "tenant_id")
    private Long tenantId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    private Set<String> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_custom_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> customRoles = new HashSet<>();

    @Column(name = "is_active")
    private boolean isActive;


    public User() {

    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public Set<Role> getCustomRoles() {
        return customRoles;
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
}

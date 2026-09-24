package app.dto;

import java.util.HashSet;
import java.util.Set;

public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String password;
    private String phoneNumber;
    private Long tenantId;
    private Set<String> roles = new HashSet<>();
    private Set<RoleDTO> customRoles = new HashSet<>();
    private boolean isActive;
    private Long primaryCategoryId;
    private String primaryCategoryName;
    // null = leave assignments untouched, empty set = clear them all
    private Set<Long> customRoleIds;

    public UserDTO() {
    }

    public UserDTO(Long id, String email, String password, String phoneNumber, Long tenantId, boolean isActive, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.tenantId = tenantId;
        this.isActive = isActive;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }


    public UserDTO(String email, Set<String> roles) {
        this.email = email;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public UserDTO(String email, Set<String> roles , boolean isActive) {
        this.email = email;
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
        this.isActive = isActive;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Set<RoleDTO> getCustomRoles() {
        return customRoles;
    }

    public void setCustomRoles(Set<RoleDTO> customRoles) {
        this.customRoles = customRoles == null ? new HashSet<>() : new HashSet<>(customRoles);
    }

    public boolean getIsActive() {
        return isActive;
    }
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }
    public Set<Long> getCustomRoleIds() {
        return customRoleIds;
    }

    public void setCustomRoleIds(Set<Long> customRoleIds) {
        this.customRoleIds = customRoleIds == null ? null : new HashSet<>(customRoleIds);
    }

    public Long getPrimaryCategoryId() {
        return primaryCategoryId;
    }

    public void setPrimaryCategoryId(Long primaryCategoryId) {
        this.primaryCategoryId = primaryCategoryId;
    }

    public String getPrimaryCategoryName() {
        return primaryCategoryName;
    }

    public void setPrimaryCategoryName(String primaryCategoryName) {
        this.primaryCategoryName = primaryCategoryName;
    }
}

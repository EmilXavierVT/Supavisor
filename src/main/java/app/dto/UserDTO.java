package app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String password;
    private String phoneNumber;
    private String primaryCategory;
    private Long tenantId;
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Set<String> roles = new HashSet<>();
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Set<RoleDTO> customRoles = new HashSet<>();
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Set<QualificationDTO> qualifications = new HashSet<>();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isActive;
    @Setter(AccessLevel.NONE)
    private Set<Long> customRoleIds;
    // null = leave assignments untouched, empty set = clear them all
    @Setter(AccessLevel.NONE)
    private Set<Long> qualificationIds;

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

    public void setRoles(Set<String> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
    }

    public void setCustomRoles(Set<RoleDTO> customRoles) {
        this.customRoles = customRoles == null ? new HashSet<>() : new HashSet<>(customRoles);
    }

    public void setQualifications(Set<QualificationDTO> qualifications) {
        this.qualifications = qualifications == null ? new HashSet<>() : new HashSet<>(qualifications);
    }

    public boolean getIsActive() {
        return isActive;
    }
    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public void setCustomRoleIds(Set<Long> customRoleIds) {
        this.customRoleIds = customRoleIds == null ? null : new HashSet<>(customRoleIds);
    }

    public void setQualificationIds(Set<Long> qualificationIds) {
        this.qualificationIds = qualificationIds == null ? null : new HashSet<>(qualificationIds);
    }
}

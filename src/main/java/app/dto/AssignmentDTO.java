package app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import app.entities.AssignmentState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDTO {

    private Long id;
    private String name;
    private Long tenantId;
    private Boolean isActive;
    private String address;
    private Integer estimatedMinutes;
    private BigDecimal cost;
    private LocalDateTime startTime;
    private LocalDateTime estimatedEndTime;
    private AssignmentState state;
    private Instant checkInAt;
    private Instant checkOutAt;
    private Long assignedEmployeeId;
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<Long> productIds = new ArrayList<>();

    public AssignmentDTO(Long id, String name, Long tenantId, Boolean isActive) {
        this.id = id;
        this.name = name;
        this.tenantId = tenantId;
        this.isActive = isActive;
    }

    public void setProductIds(List<Long> productIds) {
        this.productIds = productIds == null ? new ArrayList<>() : new ArrayList<>(productIds);
    }
}

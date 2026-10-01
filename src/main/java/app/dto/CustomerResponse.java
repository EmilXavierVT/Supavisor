package app.dto;

import java.time.LocalDateTime;
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
public class CustomerResponse {
    private Long id;
    private Long tenantId;
    private String name;
    private String email;
    private String address;
    private String postalCode;
    private String city;
    private String country;
    private String corporateIdentificationNumber;
    private String currency;
    private Integer economicCustomerNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package app.dto;

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
public class CreateCustomerRequest {
    private String name;
    private String email;
    private String address;
    private String postalCode;
    private String city;
    private String country;
    private String corporateIdentificationNumber;
    private String currency;
    private Integer customerGroupNumber;
    private Integer paymentTermsNumber;
    private Integer vatZoneNumber;
    private String idempotencyKey;
}

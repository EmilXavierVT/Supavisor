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
public class EconomicCustomerRequest {
    private String name;
    private String email;
    private String address;
    private String zip;
    private String city;
    private String country;
    private String corporateIdentificationNumber;
    private String currency;
    private CustomerGroup customerGroup;
    private PaymentTerms paymentTerms;
    private VatZone vatZone;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerGroup {
        private Integer customerGroupNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentTerms {
        private Integer paymentTermsNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VatZone {
        private Integer vatZoneNumber;
    }
}

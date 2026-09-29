package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EconomicCustomerResponse {
    private Integer customerNumber;
    private String name;
    private String email;
    private String address;
    private String zip;
    private String city;
    private String country;
    private String corporateIdentificationNumber;
    private String currency;
}

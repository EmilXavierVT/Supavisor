package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    public EconomicCustomerResponse() {
    }

    public Integer getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(Integer customerNumber) { this.customerNumber = customerNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCorporateIdentificationNumber() { return corporateIdentificationNumber; }
    public void setCorporateIdentificationNumber(String corporateIdentificationNumber) { this.corporateIdentificationNumber = corporateIdentificationNumber; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}

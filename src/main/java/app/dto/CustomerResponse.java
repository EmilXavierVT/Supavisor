package app.dto;

import java.time.LocalDateTime;

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

    public CustomerResponse() {
    }

    public CustomerResponse(Long id, Long tenantId, String name, String email, String address, String postalCode, String city,
                             String country, String corporateIdentificationNumber, String currency,
                             Integer economicCustomerNumber, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.email = email;
        this.address = address;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.corporateIdentificationNumber = corporateIdentificationNumber;
        this.currency = currency;
        this.economicCustomerNumber = economicCustomerNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCorporateIdentificationNumber() { return corporateIdentificationNumber; }
    public void setCorporateIdentificationNumber(String corporateIdentificationNumber) { this.corporateIdentificationNumber = corporateIdentificationNumber; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getEconomicCustomerNumber() { return economicCustomerNumber; }
    public void setEconomicCustomerNumber(Integer economicCustomerNumber) { this.economicCustomerNumber = economicCustomerNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

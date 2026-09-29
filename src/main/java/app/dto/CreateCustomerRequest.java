package app.dto;

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

    public CreateCustomerRequest() {
    }

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
    public Integer getCustomerGroupNumber() { return customerGroupNumber; }
    public void setCustomerGroupNumber(Integer customerGroupNumber) { this.customerGroupNumber = customerGroupNumber; }
    public Integer getPaymentTermsNumber() { return paymentTermsNumber; }
    public void setPaymentTermsNumber(Integer paymentTermsNumber) { this.paymentTermsNumber = paymentTermsNumber; }
    public Integer getVatZoneNumber() { return vatZoneNumber; }
    public void setVatZoneNumber(Integer vatZoneNumber) { this.vatZoneNumber = vatZoneNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}

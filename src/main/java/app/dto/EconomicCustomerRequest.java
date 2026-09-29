package app.dto;

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

    public EconomicCustomerRequest() {
    }

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
    public CustomerGroup getCustomerGroup() { return customerGroup; }
    public void setCustomerGroup(CustomerGroup customerGroup) { this.customerGroup = customerGroup; }
    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }
    public VatZone getVatZone() { return vatZone; }
    public void setVatZone(VatZone vatZone) { this.vatZone = vatZone; }

    public static class CustomerGroup {
        private Integer customerGroupNumber;
        public CustomerGroup() {}
        public CustomerGroup(Integer customerGroupNumber) { this.customerGroupNumber = customerGroupNumber; }
        public Integer getCustomerGroupNumber() { return customerGroupNumber; }
        public void setCustomerGroupNumber(Integer customerGroupNumber) { this.customerGroupNumber = customerGroupNumber; }
    }

    public static class PaymentTerms {
        private Integer paymentTermsNumber;
        public PaymentTerms() {}
        public PaymentTerms(Integer paymentTermsNumber) { this.paymentTermsNumber = paymentTermsNumber; }
        public Integer getPaymentTermsNumber() { return paymentTermsNumber; }
        public void setPaymentTermsNumber(Integer paymentTermsNumber) { this.paymentTermsNumber = paymentTermsNumber; }
    }

    public static class VatZone {
        private Integer vatZoneNumber;
        public VatZone() {}
        public VatZone(Integer vatZoneNumber) { this.vatZoneNumber = vatZoneNumber; }
        public Integer getVatZoneNumber() { return vatZoneNumber; }
        public void setVatZoneNumber(Integer vatZoneNumber) { this.vatZoneNumber = vatZoneNumber; }
    }
}

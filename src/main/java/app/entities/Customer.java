package app.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customers_economic_customer_number", columnNames = "economic_customer_number"),
        @UniqueConstraint(name = "uk_customers_idempotency_key", columnNames = "idempotency_key")
})
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;
    private String address;
    private String postalCode;
    private String city;
    private String country;
    private String corporateIdentificationNumber;

    @Column(nullable = false)
    private String currency;

    @Column(name = "economic_customer_number", unique = true)
    private Integer economicCustomerNumber;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Customer() {
    }

    public Customer(Long id, String name, String email, String address, String postalCode, String city, String country,
                    String corporateIdentificationNumber, String currency, Integer economicCustomerNumber,
                    String idempotencyKey, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.postalCode = postalCode;
        this.city = city;
        this.country = country;
        this.corporateIdentificationNumber = corporateIdentificationNumber;
        this.currency = currency;
        this.economicCustomerNumber = economicCustomerNumber;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void beforeCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

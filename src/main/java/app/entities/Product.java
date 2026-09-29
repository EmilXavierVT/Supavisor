package app.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_product_number", columnNames = "product_number"),
        @UniqueConstraint(name = "uk_products_idempotency_key", columnNames = "idempotency_key")
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_number", nullable = false, unique = true)
    private String productNumber;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    private BigDecimal salesPrice;
    private BigDecimal costPrice;
    private BigDecimal recommendedPrice;
    private String barCode;
    private Boolean barred;
    private OffsetDateTime economicLastUpdated;
    private Integer productGroupNumber;
    private String productGroupName;
    private Integer unitNumber;
    private String unitName;
    private String self;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(Long id, String productNumber, String name, String description, BigDecimal salesPrice,
                   BigDecimal costPrice, BigDecimal recommendedPrice, String barCode, Boolean barred,
                   OffsetDateTime economicLastUpdated, Integer productGroupNumber, String productGroupName,
                   Integer unitNumber, String unitName, String self, String idempotencyKey,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.productNumber = productNumber;
        this.name = name;
        this.description = description;
        this.salesPrice = salesPrice;
        this.costPrice = costPrice;
        this.recommendedPrice = recommendedPrice;
        this.barCode = barCode;
        this.barred = barred;
        this.economicLastUpdated = economicLastUpdated;
        this.productGroupNumber = productGroupNumber;
        this.productGroupName = productGroupName;
        this.unitNumber = unitNumber;
        this.unitName = unitName;
        this.self = self;
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
    public String getProductNumber() { return productNumber; }
    public void setProductNumber(String productNumber) { this.productNumber = productNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getSalesPrice() { return salesPrice; }
    public void setSalesPrice(BigDecimal salesPrice) { this.salesPrice = salesPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public void setRecommendedPrice(BigDecimal recommendedPrice) { this.recommendedPrice = recommendedPrice; }
    public String getBarCode() { return barCode; }
    public void setBarCode(String barCode) { this.barCode = barCode; }
    public Boolean getBarred() { return barred; }
    public void setBarred(Boolean barred) { this.barred = barred; }
    public OffsetDateTime getEconomicLastUpdated() { return economicLastUpdated; }
    public void setEconomicLastUpdated(OffsetDateTime economicLastUpdated) { this.economicLastUpdated = economicLastUpdated; }
    public Integer getProductGroupNumber() { return productGroupNumber; }
    public void setProductGroupNumber(Integer productGroupNumber) { this.productGroupNumber = productGroupNumber; }
    public String getProductGroupName() { return productGroupName; }
    public void setProductGroupName(String productGroupName) { this.productGroupName = productGroupName; }
    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getSelf() { return self; }
    public void setSelf(String self) { this.self = self; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

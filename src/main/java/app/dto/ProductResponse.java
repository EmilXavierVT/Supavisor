package app.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ProductResponse {
    private Long id;
    private Long tenantId;
    private String productNumber;
    private String name;
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

    public ProductResponse() {
    }

    public ProductResponse(Long id, Long tenantId, String productNumber, String name, String description, BigDecimal salesPrice,
                            BigDecimal costPrice, BigDecimal recommendedPrice, String barCode, Boolean barred,
                            OffsetDateTime economicLastUpdated, Integer productGroupNumber, String productGroupName,
                            Integer unitNumber, String unitName, String self) {
        this.id = id;
        this.tenantId = tenantId;
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
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
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
}

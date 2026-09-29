package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class EconomicProductResponse {
    private String productNumber;
    private String name;
    private String description;
    private BigDecimal salesPrice;
    private BigDecimal costPrice;
    private BigDecimal recommendedPrice;
    private String barCode;
    private Boolean barred;
    private OffsetDateTime lastUpdated;
    private ProductGroup productGroup;
    private Unit unit;
    private String self;

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
    public OffsetDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(OffsetDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public ProductGroup getProductGroup() { return productGroup; }
    public void setProductGroup(ProductGroup productGroup) { this.productGroup = productGroup; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public String getSelf() { return self; }
    public void setSelf(String self) { this.self = self; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductGroup {
        private Integer productGroupNumber;
        private String name;
        private String self;

        public Integer getProductGroupNumber() { return productGroupNumber; }
        public void setProductGroupNumber(Integer productGroupNumber) { this.productGroupNumber = productGroupNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSelf() { return self; }
        public void setSelf(String self) { this.self = self; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Unit {
        private Integer unitNumber;
        private String name;
        private String self;

        public Integer getUnitNumber() { return unitNumber; }
        public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSelf() { return self; }
        public void setSelf(String self) { this.self = self; }
    }
}

package app.dto;

import java.math.BigDecimal;

public class CreateProductRequest {
    private String productNumber;
    private String name;
    private String description;
    private BigDecimal salesPrice;
    private BigDecimal costPrice;
    private BigDecimal recommendedPrice;
    private String barCode;
    private Boolean barred;
    private Integer productGroupNumber;
    private Integer unitNumber;
    private String idempotencyKey;

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
    public Integer getProductGroupNumber() { return productGroupNumber; }
    public void setProductGroupNumber(Integer productGroupNumber) { this.productGroupNumber = productGroupNumber; }
    public Integer getUnitNumber() { return unitNumber; }
    public void setUnitNumber(Integer unitNumber) { this.unitNumber = unitNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}

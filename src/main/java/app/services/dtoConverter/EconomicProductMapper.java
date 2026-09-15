package app.services.dtoConverter;

import app.dto.CreateProductRequest;
import app.dto.EconomicProductRequest;
import app.dto.EconomicProductResponse;
import app.dto.ProductResponse;
import app.dto.UpdateProductRequest;
import app.entities.Product;

public class EconomicProductMapper {
    public Product fromCreateRequest(CreateProductRequest request) {
        return new Product(null, request.getProductNumber(), request.getName(), request.getDescription(),
                request.getSalesPrice(), request.getCostPrice(), request.getRecommendedPrice(), request.getBarCode(),
                request.getBarred(), null, request.getProductGroupNumber(), null, request.getUnitNumber(), null,
                null, request.getIdempotencyKey(), null, null);
    }

    public EconomicProductRequest toEconomicRequest(CreateProductRequest request) {
        EconomicProductRequest economicRequest = new EconomicProductRequest();
        applyCommonRequestFields(economicRequest, request.getProductNumber(), request.getName(), request.getDescription(),
                request.getSalesPrice(), request.getCostPrice(), request.getRecommendedPrice(), request.getBarCode(),
                request.getBarred(), request.getProductGroupNumber(), request.getUnitNumber());
        return economicRequest;
    }

    public EconomicProductRequest toEconomicRequest(String productNumber, UpdateProductRequest request) {
        EconomicProductRequest economicRequest = new EconomicProductRequest();
        String requestProductNumber = request.getProductNumber() == null || request.getProductNumber().isBlank()
                ? productNumber : request.getProductNumber();
        applyCommonRequestFields(economicRequest, requestProductNumber, request.getName(), request.getDescription(),
                request.getSalesPrice(), request.getCostPrice(), request.getRecommendedPrice(), request.getBarCode(),
                request.getBarred(), request.getProductGroupNumber(), request.getUnitNumber());
        return economicRequest;
    }

    public void applyEconomicResponse(Product product, EconomicProductResponse response) {
        product.setProductNumber(response.getProductNumber());
        product.setName(response.getName());
        product.setDescription(response.getDescription());
        product.setSalesPrice(response.getSalesPrice());
        product.setCostPrice(response.getCostPrice());
        product.setRecommendedPrice(response.getRecommendedPrice());
        product.setBarCode(response.getBarCode());
        product.setBarred(response.getBarred());
        product.setEconomicLastUpdated(response.getLastUpdated());
        product.setSelf(response.getSelf());
        if (response.getProductGroup() != null) {
            product.setProductGroupNumber(response.getProductGroup().getProductGroupNumber());
            product.setProductGroupName(response.getProductGroup().getName());
        }
        if (response.getUnit() != null) {
            product.setUnitNumber(response.getUnit().getUnitNumber());
            product.setUnitName(response.getUnit().getName());
        }
    }

    public Product toProduct(EconomicProductResponse response) {
        Product product = new Product();
        applyEconomicResponse(product, response);
        return product;
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getProductNumber(), product.getName(), product.getDescription(),
                product.getSalesPrice(), product.getCostPrice(), product.getRecommendedPrice(), product.getBarCode(),
                product.getBarred(), product.getEconomicLastUpdated(), product.getProductGroupNumber(),
                product.getProductGroupName(), product.getUnitNumber(), product.getUnitName(), product.getSelf());
    }

    private void applyCommonRequestFields(EconomicProductRequest economicRequest, String productNumber, String name,
                                          String description, java.math.BigDecimal salesPrice,
                                          java.math.BigDecimal costPrice, java.math.BigDecimal recommendedPrice,
                                          String barCode, Boolean barred, Integer productGroupNumber,
                                          Integer unitNumber) {
        economicRequest.setProductNumber(productNumber);
        economicRequest.setName(name);
        economicRequest.setDescription(description);
        economicRequest.setSalesPrice(salesPrice);
        economicRequest.setCostPrice(costPrice);
        economicRequest.setRecommendedPrice(recommendedPrice);
        economicRequest.setBarCode(barCode);
        economicRequest.setBarred(barred);
        if (productGroupNumber != null) {
            economicRequest.setProductGroup(new EconomicProductRequest.ProductGroup(productGroupNumber));
        }
        if (unitNumber != null) {
            economicRequest.setUnit(new EconomicProductRequest.Unit(unitNumber));
        }
    }
}

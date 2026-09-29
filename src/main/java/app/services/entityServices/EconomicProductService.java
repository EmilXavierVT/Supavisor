package app.services.entityServices;

import app.dao.ProductDAO;
import app.dto.CreateProductRequest;
import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductRequest;
import app.dto.EconomicProductResponse;
import app.dto.ProductResponse;
import app.dto.UpdateProductRequest;
import app.entities.Product;
import app.exceptions.ApiException;
import app.services.dtoConverter.EconomicProductMapper;
import app.services.economic.EconomicProductClient;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class EconomicProductService {
    private final ProductDAO productDAO;
    private final EconomicProductMapper mapper;
    private final EconomicProductClient client;

    public EconomicProductService(EntityManagerFactory emf) {
        this(new ProductDAO(emf), new EconomicProductMapper(), new EconomicProductClient());
    }

    public EconomicProductService(ProductDAO productDAO, EconomicProductMapper mapper, EconomicProductClient client) {
        this.productDAO = productDAO;
        this.mapper = mapper;
        this.client = client;
    }

    public List<ProductResponse> listProducts(int pageSize, int skipPages) {
        int safePageSize = validatePageSize(pageSize);
        int safeSkipPages = validateSkipPages(skipPages);
        EconomicProductListResponse economicResponse = client.listProducts(safePageSize, safeSkipPages);
        return economicResponse.getCollection().stream()
                .map(this::saveEconomicProduct)
                .map(mapper::toResponse)
                .toList();
    }

    public ProductResponse getProduct(String productNumber) {
        validateProductNumber(productNumber);
        EconomicProductResponse economicResponse = client.getProduct(productNumber);
        return mapper.toResponse(saveEconomicProduct(economicResponse));
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        validateCreateRequest(request);
        String idempotencyKey = stableIdempotencyKey(request);
        request.setIdempotencyKey(idempotencyKey);

        Product existingByKey = productDAO.findByIdempotencyKey(idempotencyKey);
        if (existingByKey != null && existingByKey.getSelf() != null) {
            return mapper.toResponse(existingByKey);
        }

        Product localProduct = existingByKey == null ? mapper.fromCreateRequest(request) : existingByKey;
        if (existingByKey == null) {
            localProduct = productDAO.save(localProduct);
        }

        EconomicProductRequest economicRequest = mapper.toEconomicRequest(request);
        EconomicProductResponse economicResponse = client.createProduct(economicRequest, idempotencyKey);
        mapper.applyEconomicResponse(localProduct, economicResponse);
        return mapper.toResponse(productDAO.save(localProduct));
    }

    public ProductResponse updateProduct(String productNumber, UpdateProductRequest request) {
        validateUpdateRequest(productNumber, request);
        EconomicProductRequest economicRequest = mapper.toEconomicRequest(productNumber, request);
        EconomicProductResponse economicResponse = client.updateProduct(productNumber, economicRequest);
        return mapper.toResponse(saveEconomicProduct(economicResponse));
    }

    private Product saveEconomicProduct(EconomicProductResponse response) {
        if (response.getProductNumber() == null || response.getProductNumber().isBlank()) {
            throw new ApiException(502, "e-conomic did not return a product number");
        }
        Product product = productDAO.findByProductNumber(response.getProductNumber());
        if (product == null) {
            product = mapper.toProduct(response);
        } else {
            mapper.applyEconomicResponse(product, response);
        }
        return productDAO.save(product);
    }

    private void validateCreateRequest(CreateProductRequest request) {
        if (request == null) throw new ApiException(400, "Request body is required");
        validateProductNumber(request.getProductNumber());
        if (isBlank(request.getName())) throw new ApiException(400, "name is required");
        if (request.getProductGroupNumber() == null) throw new ApiException(400, "productGroupNumber is required");
    }

    private void validateUpdateRequest(String productNumber, UpdateProductRequest request) {
        validateProductNumber(productNumber);
        if (request == null) throw new ApiException(400, "Request body is required");
        if (request.getProductNumber() != null && !request.getProductNumber().isBlank() && !productNumber.equals(request.getProductNumber())) {
            throw new ApiException(400, "productNumber in path and body must match");
        }
        if (isBlank(request.getName())) throw new ApiException(400, "name is required");
        if (request.getProductGroupNumber() == null) throw new ApiException(400, "productGroupNumber is required");
    }

    private void validateProductNumber(String productNumber) {
        if (productNumber == null || productNumber.isBlank()) {
            throw new ApiException(400, "productNumber is required");
        }
    }

    private String stableIdempotencyKey(CreateProductRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return request.getIdempotencyKey();
        }
        return "product-" + request.getProductNumber();
    }

    private int validatePageSize(int pageSize) {
        if (pageSize < 1) return 20;
        if (pageSize > 1000) return 1000;
        return pageSize;
    }

    private int validateSkipPages(int skipPages) {
        if (skipPages < 0) return 0;
        return skipPages;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

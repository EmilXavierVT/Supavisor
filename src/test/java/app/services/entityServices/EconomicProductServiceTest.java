package app.services.entityServices;

import app.config.TestEntityManagerFactory;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class EconomicProductServiceTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("supavisor_economic_product_service_test")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setUp() {
        emf = TestEntityManagerFactory.create(POSTGRES);
    }

    @AfterAll
    static void tearDown() {
        if (emf != null) emf.close();
    }

    @Test
    void listsProductsAndPersistsReturnedProducts() {
        ProductDAO dao = new ProductDAO(emf);
        FakeClient client = new FakeClient();
        client.listResponse = List.of(economicProduct("P-SVC-1", "Listed Product"));
        EconomicProductService service = new EconomicProductService(dao, new EconomicProductMapper(), client);

        List<ProductResponse> products = service.listProducts(20, 0);

        assertEquals(1, products.size());
        assertEquals("P-SVC-1", products.get(0).getProductNumber());
        assertNotNull(dao.findByProductNumber("P-SVC-1"));
    }

    @Test
    void getsProductAndPersistsReturnedProduct() {
        ProductDAO dao = new ProductDAO(emf);
        FakeClient client = new FakeClient();
        client.getResponse = economicProduct("P-SVC-2", "Fetched Product");
        EconomicProductService service = new EconomicProductService(dao, new EconomicProductMapper(), client);

        ProductResponse response = service.getProduct("P-SVC-2");

        assertEquals("P-SVC-2", response.getProductNumber());
        assertNotNull(dao.findByProductNumber("P-SVC-2"));
    }

    @Test
    void createsProductWithStableIdempotencyKeyAndPersistsReturnedProduct() {
        ProductDAO dao = new ProductDAO(emf);
        FakeClient client = new FakeClient();
        client.createResponse = economicProduct("P-SVC-3", "Created Product");
        EconomicProductService service = new EconomicProductService(dao, new EconomicProductMapper(), client);

        ProductResponse response = service.createProduct(validCreateRequest("P-SVC-3"));

        Product saved = dao.findByProductNumber("P-SVC-3");
        assertEquals("P-SVC-3", response.getProductNumber());
        assertEquals("product-P-SVC-3", client.lastIdempotencyKey);
        assertEquals(saved.getId(), response.getId());
    }

    @Test
    void updatesProductAndPersistsReturnedProduct() {
        ProductDAO dao = new ProductDAO(emf);
        dao.save(new Product(null, "P-SVC-4", "Old", null, null, null, null, null, false, null,
                1, null, null, null, null, "idem-product-4", null, null));
        FakeClient client = new FakeClient();
        client.updateResponse = economicProduct("P-SVC-4", "Updated Product");
        EconomicProductService service = new EconomicProductService(dao, new EconomicProductMapper(), client);

        UpdateProductRequest request = validUpdateRequest("P-SVC-4");
        ProductResponse response = service.updateProduct("P-SVC-4", request);

        assertEquals("Updated Product", response.getName());
        assertEquals("P-SVC-4", client.lastUpdatedProductNumber);
    }

    @Test
    void rejectsMissingRequiredFields() {
        EconomicProductService service = new EconomicProductService(new ProductDAO(emf), new EconomicProductMapper(), new FakeClient());
        CreateProductRequest request = validCreateRequest("P-SVC-5");
        request.setName(null);

        ApiException exception = assertThrows(ApiException.class, () -> service.createProduct(request));

        assertEquals(400, exception.getCode());
    }

    private CreateProductRequest validCreateRequest(String productNumber) {
        CreateProductRequest request = new CreateProductRequest();
        request.setProductNumber(productNumber);
        request.setName("Example Product");
        request.setDescription("Description");
        request.setSalesPrice(new BigDecimal("100.00"));
        request.setCostPrice(new BigDecimal("50.00"));
        request.setProductGroupNumber(1);
        request.setUnitNumber(1);
        return request;
    }

    private UpdateProductRequest validUpdateRequest(String productNumber) {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setProductNumber(productNumber);
        request.setName("Updated Product");
        request.setDescription("Updated description");
        request.setSalesPrice(new BigDecimal("110.00"));
        request.setProductGroupNumber(1);
        request.setUnitNumber(1);
        return request;
    }

    private EconomicProductResponse economicProduct(String productNumber, String name) {
        EconomicProductResponse response = new EconomicProductResponse();
        response.setProductNumber(productNumber);
        response.setName(name);
        response.setSalesPrice(new BigDecimal("100.00"));
        response.setSelf("https://restapi.e-conomic.com/products/" + productNumber);
        EconomicProductResponse.ProductGroup productGroup = new EconomicProductResponse.ProductGroup();
        productGroup.setProductGroupNumber(1);
        productGroup.setName("Goods");
        response.setProductGroup(productGroup);
        return response;
    }

    private static class FakeClient extends EconomicProductClient {
        private List<EconomicProductResponse> listResponse = List.of();
        private EconomicProductResponse getResponse;
        private EconomicProductResponse createResponse;
        private EconomicProductResponse updateResponse;
        private String lastIdempotencyKey;
        private String lastUpdatedProductNumber;

        private FakeClient() {
            super(HttpClient.newHttpClient(), "http://localhost", "app", "agreement");
        }

        @Override
        public EconomicProductListResponse listProducts(int pageSize, int skipPages) {
            EconomicProductListResponse response = new EconomicProductListResponse();
            response.setCollection(listResponse);
            return response;
        }

        @Override
        public EconomicProductResponse getProduct(String productNumber) {
            return getResponse;
        }

        @Override
        public EconomicProductResponse createProduct(EconomicProductRequest productRequest, String idempotencyKey) {
            lastIdempotencyKey = idempotencyKey;
            return createResponse;
        }

        @Override
        public EconomicProductResponse updateProduct(String productNumber, EconomicProductRequest productRequest) {
            lastUpdatedProductNumber = productNumber;
            return updateResponse;
        }
    }
}

package app.services.dtoConverter;

import app.dto.CreateProductRequest;
import app.dto.EconomicProductRequest;
import app.dto.EconomicProductResponse;
import app.dto.ProductResponse;
import app.entities.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomicProductMapperTest {

    @Test
    void mapsCreateRequestToEconomicRequest() {
        CreateProductRequest request = new CreateProductRequest();
        request.setProductNumber("P-1");
        request.setName("Example");
        request.setDescription("Description");
        request.setSalesPrice(new BigDecimal("100.00"));
        request.setCostPrice(new BigDecimal("50.00"));
        request.setRecommendedPrice(new BigDecimal("120.00"));
        request.setBarCode("1234567890");
        request.setBarred(false);
        request.setProductGroupNumber(1);
        request.setUnitNumber(2);

        EconomicProductRequest economicRequest = new EconomicProductMapper().toEconomicRequest(request);

        assertEquals("P-1", economicRequest.getProductNumber());
        assertEquals("Example", economicRequest.getName());
        assertEquals(new BigDecimal("100.00"), economicRequest.getSalesPrice());
        assertEquals(1, economicRequest.getProductGroup().getProductGroupNumber());
        assertEquals(2, economicRequest.getUnit().getUnitNumber());
    }

    @Test
    void appliesEconomicResponseToEntityAndPublicResponse() {
        EconomicProductResponse economicResponse = new EconomicProductResponse();
        economicResponse.setProductNumber("P-2");
        economicResponse.setName("Example");
        economicResponse.setSalesPrice(new BigDecimal("100.00"));
        economicResponse.setLastUpdated(OffsetDateTime.parse("2026-01-01T10:15:30Z"));
        economicResponse.setSelf("https://restapi.e-conomic.com/products/P-2");

        EconomicProductResponse.ProductGroup productGroup = new EconomicProductResponse.ProductGroup();
        productGroup.setProductGroupNumber(1);
        productGroup.setName("Goods");
        economicResponse.setProductGroup(productGroup);

        EconomicProductResponse.Unit unit = new EconomicProductResponse.Unit();
        unit.setUnitNumber(2);
        unit.setName("pcs");
        economicResponse.setUnit(unit);

        EconomicProductMapper mapper = new EconomicProductMapper();
        Product product = mapper.toProduct(economicResponse);
        ProductResponse response = mapper.toResponse(product);

        assertEquals("P-2", response.getProductNumber());
        assertEquals("Example", response.getName());
        assertEquals(1, response.getProductGroupNumber());
        assertEquals("Goods", response.getProductGroupName());
        assertEquals(2, response.getUnitNumber());
        assertEquals("pcs", response.getUnitName());
        assertEquals("https://restapi.e-conomic.com/products/P-2", response.getSelf());
    }
}

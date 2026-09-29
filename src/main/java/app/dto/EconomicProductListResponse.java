package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class EconomicProductListResponse {
    private List<EconomicProductResponse> collection = new ArrayList<>();
    private JsonNode pagination;
    private JsonNode self;

    public List<EconomicProductResponse> getCollection() { return collection; }
    public void setCollection(List<EconomicProductResponse> collection) { this.collection = collection == null ? new ArrayList<>() : collection; }
    public JsonNode getPagination() { return pagination; }
    public void setPagination(JsonNode pagination) { this.pagination = pagination; }
    public JsonNode getSelf() { return self; }
    public void setSelf(JsonNode self) { this.self = self; }
}

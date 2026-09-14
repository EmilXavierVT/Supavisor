package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EconomicCustomerListResponse {
    private List<EconomicCustomerResponse> collection = new ArrayList<>();
    private JsonNode pagination;
    private JsonNode self;

    public EconomicCustomerListResponse() {
    }

    public List<EconomicCustomerResponse> getCollection() { return collection; }
    public void setCollection(List<EconomicCustomerResponse> collection) { this.collection = collection == null ? new ArrayList<>() : collection; }
    public JsonNode getPagination() { return pagination; }
    public void setPagination(JsonNode pagination) { this.pagination = pagination; }
    public JsonNode getSelf() { return self; }
    public void setSelf(JsonNode self) { this.self = self; }
}

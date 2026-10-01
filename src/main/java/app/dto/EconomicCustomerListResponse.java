package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EconomicCustomerListResponse {
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<EconomicCustomerResponse> collection = new ArrayList<>();
    private JsonNode pagination;
    private JsonNode self;

    public void setCollection(List<EconomicCustomerResponse> collection) { this.collection = collection == null ? new ArrayList<>() : collection; }
}

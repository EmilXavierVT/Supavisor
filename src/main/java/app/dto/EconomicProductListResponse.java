package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EconomicProductListResponse {
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private List<EconomicProductResponse> collection = new ArrayList<>();
    private JsonNode pagination;
    private JsonNode self;

    public void setCollection(List<EconomicProductResponse> collection) { this.collection = collection == null ? new ArrayList<>() : collection; }
}

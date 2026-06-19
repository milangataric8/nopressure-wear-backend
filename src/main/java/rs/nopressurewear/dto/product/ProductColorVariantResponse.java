package rs.nopressurewear.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductColorVariantResponse {
    private Long productId;
    private String name;
    private String colorName;
    private String colorHex;
    private String imageUrl;
    private String sku;
    @JsonProperty("isCurrent")
    private boolean isCurrent;
}

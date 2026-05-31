package rs.webshop.webshop_core.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProductColorVariantResponse {
    private Long variantId;
    private String imageUrl;
}

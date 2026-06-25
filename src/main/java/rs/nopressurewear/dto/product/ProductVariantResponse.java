package rs.nopressurewear.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.model.ProductSize;

@Getter
@Setter
@Builder
public class ProductVariantResponse {
    private Long id;
    private ProductSize size;
    private Integer stockQuantity;
    private String sku;
    private boolean inStock;
}

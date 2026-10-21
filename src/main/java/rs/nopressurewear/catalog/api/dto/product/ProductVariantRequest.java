package rs.nopressurewear.catalog.api.dto.product;

import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.catalog.api.ProductSize;

@Getter
@Setter
public class ProductVariantRequest {
    private ProductSize size;
    private Integer stockQuantity;
    private String sku;
}

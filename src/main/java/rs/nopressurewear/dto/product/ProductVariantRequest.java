package rs.nopressurewear.dto.product;

import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.constants.ProductSize;

@Getter
@Setter
public class ProductVariantRequest {
    private ProductSize size;
    private Integer stockQuantity;
    private String sku;
}

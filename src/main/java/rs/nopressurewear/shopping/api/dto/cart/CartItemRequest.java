package rs.nopressurewear.shopping.api.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rs.nopressurewear.catalog.api.ProductSize;

@Getter
@Setter
public class CartItemRequest {

    @NotNull(message = "validation.productId.required")
    private Long productId;

    @NotNull(message = "validation.quantity.required")
    @Min(value = 1, message = "validation.quantity.min")
    private Integer quantity;

    private ProductSize size;
}
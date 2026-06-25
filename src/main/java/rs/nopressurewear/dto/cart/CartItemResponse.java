package rs.nopressurewear.dto.cart;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import rs.nopressurewear.model.ProductSize;

@Getter
@Setter
@Builder
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;
    private BigDecimal discountPrice;
    private BigDecimal discountPercentage;
    private ProductSize size;
}
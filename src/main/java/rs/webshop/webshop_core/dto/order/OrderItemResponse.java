package rs.webshop.webshop_core.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal;
    private String imageUrl;
}
package rs.nopressurewear.ordering.api.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import rs.nopressurewear.catalog.api.ProductSize;

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
    private String orderCode;
    private ProductSize size;
}
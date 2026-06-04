package rs.webshop.webshop_core.dto.favorite;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.webshop.webshop_core.dto.product.ProductResponse;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FavoriteResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private java.math.BigDecimal productPrice;
    private Boolean productInStock;
    private LocalDateTime createdAt;
}
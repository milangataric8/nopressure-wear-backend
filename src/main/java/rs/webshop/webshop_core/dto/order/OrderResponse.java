package rs.webshop.webshop_core.dto.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import rs.webshop.webshop_core.constants.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class OrderResponse {

    private Long id;
    private Long userId;
    private List<OrderItemResponse> orderItems;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
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
    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String shippingStreet;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    private List<OrderItemResponse> orderItems;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

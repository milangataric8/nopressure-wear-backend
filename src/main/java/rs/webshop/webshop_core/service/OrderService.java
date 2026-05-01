package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.webshop.webshop_core.constants.OrderStatus;
import rs.webshop.webshop_core.dto.order.OrderItemResponse;
import rs.webshop.webshop_core.dto.order.OrderResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.*;
import rs.webshop.webshop_core.repository.CartRepository;
import rs.webshop.webshop_core.repository.OrderRepository;
import rs.webshop.webshop_core.repository.ProductRepository;
import rs.webshop.webshop_core.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static rs.webshop.webshop_core.constants.OrderStatus.PENDING;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Transactional
    public OrderResponse checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = Order.builder()
                .user(user)
                .status(PENDING)
                .totalAmount(ZERO)
                .build();

        List<OrderItem> orderItems = cart.getCartItems().stream().map(cartItem -> {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for product: " + product.getName());
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();
        }).toList();

        order.setOrderItems(orderItems);

        BigDecimal total = orderItems.stream()
                .map(item -> item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        cart.getCartItems().clear();
        cartRepository.save(cart);

        return toResponse(orderRepository.save(order));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public List<OrderResponse> getByUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public OrderResponse getById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Order does not belong to this user");
        }

        return toResponse(order);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .orderItems(items)
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase())
                .subtotal(item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
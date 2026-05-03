package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShippingAddress shippingAddress = getShippingAddress(user);
        Order order = createOrder(user, shippingAddress);

        if (shippingAddress != null) {
            order.setShippingAddress(shippingAddress);
        }

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
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        cart.getCartItems().clear();
        cartRepository.save(cart);

        return toResponse(orderRepository.save(order));
    }

    private static ShippingAddress getShippingAddress(User user) {
        ShippingAddress shippingAddress = null;
        if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
            Address firstAddress = user.getAddresses().get(0);
            shippingAddress = ShippingAddress.builder()
                    .street(firstAddress.getStreet())
                    .city(firstAddress.getCity())
                    .postalCode(firstAddress.getPostalCode())
                    .country(firstAddress.getCountry())
                    .build();
        }
        return shippingAddress;
    }

    private static Order createOrder(User user, ShippingAddress shippingAddress) {
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .customerFirstName(user.getFirstName())
                .customerLastName(user.getLastName())
                .customerEmail(user.getEmail())
                .shippingAddress(shippingAddress)
                .build();
        return order;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or @authUtil.getCurrentUserId().equals(#userId)")
    public Page<OrderResponse> getByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
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
                .customerFirstName(order.getCustomerFirstName())
                .customerLastName(order.getCustomerLastName())
                .customerEmail(order.getCustomerEmail())
                .shippingStreet(nonNull(order.getShippingAddress())
                        ? order.getShippingAddress().getStreet()
                        : null)
                .shippingCity(nonNull(order.getShippingAddress())
                        ? order.getShippingAddress().getCity()
                        : null)
                .shippingPostalCode(nonNull(order.getShippingAddress())
                        ? order.getShippingAddress().getPostalCode()
                        : null)
                .shippingCountry(nonNull(order.getShippingAddress())
                        ? order.getShippingAddress().getCountry()
                        : null)
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
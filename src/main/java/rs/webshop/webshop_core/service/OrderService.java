package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.webshop.webshop_core.constants.OrderStatus;
import rs.webshop.webshop_core.dto.order.GuestOrderRequest;
import rs.webshop.webshop_core.dto.order.OrderItemResponse;
import rs.webshop.webshop_core.dto.order.OrderResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.*;
import rs.webshop.webshop_core.repository.*;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;
import static rs.webshop.webshop_core.constants.OrderStatus.PENDING;

@Service
@RequiredArgsConstructor
public class OrderService {

    Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final EmailService emailService;

    private record ProductWithQuantity(Product product, int quantity) {}

    @Transactional
    public OrderResponse checkout(Long userId, String couponCode, String paymentMethod) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = createOrder(user);

        List<ProductWithQuantity> pairs = cart.getCartItems().stream()
                .map(cartItem -> new ProductWithQuantity(cartItem.getProduct(), cartItem.getQuantity()))
                .toList();

        order.setOrderItems(buildOrderItems(pairs, order));

        BigDecimal total = calculateTotalOrderValue(order.getOrderItems());
        total = applyCouponToOrder(order, couponCode, total);
        setPaymentFields(order, paymentMethod);
        order.setTotalAmount(total);

        cart.getCartItems().clear();
        cartRepository.save(cart);

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse guestCheckout(GuestOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Order must have items");
        }

        Order order = Order.builder()
                .user(null)
                .status(PENDING)
                .totalAmount(ZERO)
                .discountAmount(ZERO)
                .customerFullName(request.getCustomerFullName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .shippingAddress(ShippingAddress.builder()
                        .street(request.getStreet())
                        .city(request.getCity())
                        .postalCode(request.getPostalCode())
                        .country(request.getCountry())
                        .build())
                .orderCode(generateOrderCode())
                .build();

        List<ProductWithQuantity> pairs = request.getItems().stream()
                .map(reqItem -> {
                    Product product = productRepository.findById(reqItem.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + reqItem.getProductId()));
                    return new ProductWithQuantity(product, reqItem.getQuantity());
                })
                .toList();

        order.setOrderItems(buildOrderItems(pairs, order));

        BigDecimal total = calculateTotalOrderValue(order.getOrderItems());
        total = applyCouponToOrder(order, request.getCouponCode(), total);
        setPaymentFields(order, request.getPaymentMethod());
        order.setTotalAmount(total);

        return toResponse(orderRepository.save(order));
    }

    private List<OrderItem> buildOrderItems(List<ProductWithQuantity> pairs, Order order) {
        return pairs.stream().map(pair -> {
            Product product = pair.product();
            if (product.getStockQuantity() < pair.quantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - pair.quantity());
            product.setSalesCount((product.getSalesCount() != null ? product.getSalesCount() : 0) + pair.quantity());
            productRepository.save(product);

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(pair.quantity())
                    .priceAtPurchase(nonNull(product.getDiscountPrice())
                            ? product.getDiscountPrice()
                            : product.getPrice())
                    .build();
        }).toList();
    }

    private BigDecimal applyCouponToOrder(Order order, String couponCode, BigDecimal total) {
        if (!nonNull(couponCode) || couponCode.isBlank()) return total;

        Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (!coupon.isActive() || coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new RuntimeException("Coupon is not valid");
        }

        BigDecimal discountAmount = CouponService.applyCouponDiscount(coupon, total);
        order.setDiscountAmount(discountAmount);
        order.setCouponCode(couponCode.toUpperCase());
        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);

        return total.subtract(discountAmount);
    }

    private static void setPaymentFields(Order order, String paymentMethod) {
        order.setPaymentMethod(paymentMethod != null ? paymentMethod : "COD");
        order.setPaymentStatus("CARD".equals(paymentMethod) ? "PAID" : "PENDING");
    }

    private static BigDecimal calculateTotalOrderValue(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(ZERO, BigDecimal::add);
    }

    private static Order createOrder(User user) {
        return Order.builder()
                .user(user)
                .status(PENDING)
                .totalAmount(ZERO)
                .discountAmount(ZERO)
                .customerFullName(user.getFirstName() + " " + user.getLastName())
                .customerEmail(user.getEmail())
                .shippingAddress(getShippingAddress(user))
                .orderCode(generateOrderCode())
                .build();
    }

    private static ShippingAddress getShippingAddress(User user) {
        ShippingAddress shippingAddress = null;
        if (nonNull(user.getAddresses()) && !user.getAddresses().isEmpty()) {
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

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<OrderResponse> getAll(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<OrderResponse> search(String search, String status, Pageable pageable) {
        String orderStatus = (nonNull(status) && !status.isBlank())
                ? status
                : null;

        String searchParam = (nonNull(search) && !search.isBlank())
                ? search
                : null;

        return orderRepository.findByFilters(orderStatus, searchParam, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or @authUtil.getCurrentUserId().equals(#userId)")
    public Page<OrderResponse> getByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public Page<OrderResponse> getByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public OrderResponse getByIdAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE') or #userId == authentication.principal.id")
    public OrderResponse getById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Order does not belong to this user");
        }

        return toResponse(order);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);

        try {
            StringBuilder productRows = new StringBuilder();
            for (OrderItem item : order.getOrderItems()) {
                productRows.append("""
                <div class="item-row">
                    <div>
                        <p class="item-name">%s</p>
                        <p class="item-qty">Qty: %d × $%s</p>
                    </div>
                    <span class="item-price">$%s</span>
                </div>
                """.formatted(
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getPriceAtPurchase(),
                                item.getPriceAtPurchase().multiply(
                                        java.math.BigDecimal.valueOf(item.getQuantity())
                                )
                        )
                );
            }

            String shippingStreet = getShippingAddressPart(order, order.getShippingAddress().getStreet());
            String shippingCity = getShippingAddressPart(order, order.getShippingAddress().getCity());
            String shippingPostalCode = getShippingAddressPart(order, order.getShippingAddress().getPostalCode());
            String shippingCountry = getShippingAddressPart(order, order.getShippingAddress().getCountry());

            emailService.sendOrderStatusEmail(
                    order.getUser().getEmail(),
                    orderId,
                    status.name(),
                    order.getCustomerFullName(),
                    productRows.toString(),
                    order.getTotalAmount().toString(),
                    shippingStreet,
                    shippingCity,
                    shippingPostalCode,
                    shippingCountry
            );
        } catch (Exception e) {
            log.error("Failed to send email: " + e.getMessage());
        }

        return toResponse(savedOrder);
    }

    private static String getShippingAddressPart(Order order, String addressPart) {
        return nonNull(order.getShippingAddress()) ? addressPart : "";
    }

    private static String generateOrderCode() {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return uuid.substring(0, 8);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(nonNull(order.getUser()) ? order.getUser().getId() : null)
                .customerFullName(order.getCustomerFullName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
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
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
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
                .imageUrl(item.getProduct().getImageUrl())
                .build();
    }
}
package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.constants.OrderStatus;
import rs.nopressurewear.constants.ProductSize;
import rs.nopressurewear.dto.order.GuestOrderRequest;
import rs.nopressurewear.dto.order.OrderItemResponse;
import rs.nopressurewear.dto.order.OrderResponse;
import rs.nopressurewear.exception.EmailNotVerifiedException;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.*;
import rs.nopressurewear.repository.*;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static rs.nopressurewear.constants.OrderStatus.CANCELLED;
import static rs.nopressurewear.constants.OrderStatus.PENDING;

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
    private final ProductVariantRepository productVariantRepository;
    private final DeliveryService deliveryService;
    private final LowStockService lowStockService;

    private record ProductWithQuantity(Long productId, int quantity, ProductSize size) {}

    @Transactional
    public OrderResponse checkout(Long userId, String couponCode, String paymentMethod, String paymentIntentId, String lang) {
        Cart cart = cartRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before placing an order");
        }

        Order order = createOrder(user);

        List<ProductWithQuantity> pairs = cart.getCartItems().stream()
                .map(cartItem -> new ProductWithQuantity(cartItem.getProduct().getId(), cartItem.getQuantity(), cartItem.getSize()))
                .toList();

        order.setOrderItems(buildOrderItems(pairs, order));

        BigDecimal total = calculateTotalOrderValue(order.getOrderItems());
        total = applyCouponToOrder(order, couponCode, total);
        BigDecimal deliveryFee = deliveryService.calculateDeliveryFee(total);
        order.setDeliveryFee(deliveryFee);
        setPaymentFields(order, paymentMethod, paymentIntentId);
        order.setTotalAmount(total.add(deliveryFee));

        cart.getCartItems().clear();
        cartRepository.save(cart);

        sendOrderConfirmation(order, lang);

        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse guestCheckout(GuestOrderRequest request, String lang) {
        if (isNull(request.getItems()) || request.getItems().isEmpty()) {
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
                .map(reqItem -> new ProductWithQuantity(reqItem.getProductId(), reqItem.getQuantity(), reqItem.getSize()))
                .toList();

        order.setOrderItems(buildOrderItems(pairs, order));

        BigDecimal total = calculateTotalOrderValue(order.getOrderItems());
        total = applyCouponToOrder(order, request.getCouponCode(), total);
        BigDecimal guestDeliveryFee = deliveryService.calculateDeliveryFee(total);
        order.setDeliveryFee(guestDeliveryFee);
        setPaymentFields(order, request.getPaymentMethod(), null);
        order.setTotalAmount(total.add(guestDeliveryFee));

        sendOrderConfirmation(order, lang);

        return toResponse(orderRepository.save(order));
    }

    private List<OrderItem> buildOrderItems(List<ProductWithQuantity> pairs, Order order) {
        return pairs.stream().map(pair -> {
            Product product = productRepository.findByIdForUpdate(pair.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + pair.productId()));

            if (nonNull(pair.size())) {
                ProductVariant variant = productVariantRepository
                        .findWithLockByProductIdAndSize(product.getId(), pair.size())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Size " + pair.size() + " not available for: " + product.getName()));

                if (variant.getStockQuantity() < pair.quantity()) {
                    throw new FieldValidationException("validation.outOfStock", "size");
                }

                variant.setStockQuantity(variant.getStockQuantity() - pair.quantity());
                productVariantRepository.save(variant);
                lowStockService.checkAndAlertVariant(product, variant);
            }

            product.setSalesCount((nonNull(product.getSalesCount()) ? product.getSalesCount() : 0) + pair.quantity());
            productRepository.save(product);

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImageUrl(product.getImageUrl())
                    .quantity(pair.quantity())
                    .priceAtPurchase(nonNull(product.getDiscountPrice())
                            ? product.getDiscountPrice()
                            : product.getPrice())
                    .size(pair.size())
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

    private static void setPaymentFields(Order order, String paymentMethod, String paymentIntentId) {
        order.setPaymentMethod(nonNull(paymentMethod) ? paymentMethod : "COD");
        if ("CARD".equals(paymentMethod)) {
            if (nonNull(paymentIntentId) && !paymentIntentId.isBlank()) {
                // Webhook will confirm payment; keep PENDING_PAYMENT until payment_intent.succeeded fires
                order.setStripePaymentId(paymentIntentId);
                order.setPaymentStatus("PENDING_PAYMENT");
            } else {
                order.setPaymentStatus("PAID");
            }
        } else {
            order.setPaymentStatus("PENDING");
        }
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

    private void sendOrderConfirmation(Order order, String lang) {
        try {
            StringBuilder productRows = new StringBuilder();
            for (OrderItem item : order.getOrderItems()) {
                buildProductRowHtml(item, productRows, BigDecimal.valueOf(item.getQuantity()));
            }

            String street = nonNull(order.getShippingAddress()) ? order.getShippingAddress().getStreet() : "";
            String city = nonNull(order.getShippingAddress()) ? order.getShippingAddress().getCity() : "";
            String postalCode = nonNull(order.getShippingAddress()) ? order.getShippingAddress().getPostalCode() : "";
            String country = nonNull(order.getShippingAddress()) ? order.getShippingAddress().getCountry() : "";

            emailService.sendOrderStatusEmail(
                    order.getCustomerEmail(),
                    order.getId(),
                    order.getOrderCode(),
                    order.getStatus().name(),
                    order.getCustomerFullName(),
                    productRows.toString(),
                    order.getTotalAmount().toString(),
                    street,
                    city,
                    postalCode,
                    country,
                    order.getDeliveryFee(),
                    lang
            );
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: " + e.getMessage());
        }
    }

    private void buildProductRowHtml(OrderItem item, StringBuilder productRows, BigDecimal quantity) {
        String imageUrl = nonNull(item.getProductImageUrl())
                ? item.getProductImageUrl()
                : (nonNull(item.getProduct()) ? item.getProduct().getImageUrl() : null);

        String resolvedImageUrl = emailService.resolveImageUrl(imageUrl);
        String imageHtml = nonNull(resolvedImageUrl)
                ? "<img src=\"" + resolvedImageUrl + "\" alt=\"\" class=\"item-img\" />"
                : "<div class=\"item-img\"></div>";

        String productName = nonNull(item.getProductName())
                ? item.getProductName()
                : (nonNull(item.getProduct()) ? item.getProduct().getName() : "—");

        String sizeLabel = nonNull(item.getSize()) ? " | Size: " + item.getSize() : "";
        productRows.append("""
                <div class="item-row">
                    %s
                    <div>
                        <p class="item-name">%s</p>
                        <p class="item-qty">Qty: %d × %s RSD%s</p>
                    </div>
                    <span class="item-price">%s RSD</span>
                </div>
                """.formatted(
                imageHtml,
                productName,
                item.getQuantity(),
                item.getPriceAtPurchase(),
                sizeLabel,
                item.getPriceAtPurchase().multiply(quantity)
        ));
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

        PageRequest unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findByFilters(orderStatus, searchParam, unsorted)
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
    public OrderResponse updateStatus(Long orderId, OrderStatus status, String lang) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus previousStatus = order.getStatus();

        restoreStockIfOrderCanceled(status, previousStatus, order);

        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);

        try {
            StringBuilder productRows = new StringBuilder();
            for (OrderItem item : order.getOrderItems()) {
                buildProductRowHtml(item, productRows, BigDecimal.valueOf(item.getQuantity()));
            }

            String shippingStreet = getShippingAddressPart(order, order.getShippingAddress().getStreet());
            String shippingCity = getShippingAddressPart(order, order.getShippingAddress().getCity());
            String shippingPostalCode = getShippingAddressPart(order, order.getShippingAddress().getPostalCode());
            String shippingCountry = getShippingAddressPart(order, order.getShippingAddress().getCountry());

            emailService.sendOrderStatusEmail(
                    order.getCustomerEmail(),
                    orderId,
                    order.getOrderCode(),
                    status.name(),
                    order.getCustomerFullName(),
                    productRows.toString(),
                    order.getTotalAmount().toString(),
                    shippingStreet,
                    shippingCity,
                    shippingPostalCode,
                    shippingCountry,
                    order.getDeliveryFee(),
                    lang
            );
        } catch (Exception e) {
            log.error("Failed to send email: " + e.getMessage());
        }

        return toResponse(savedOrder);
    }

    private void restoreStockIfOrderCanceled(OrderStatus status, OrderStatus previousStatus, Order order) {
        if (status == CANCELLED && previousStatus != CANCELLED) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (nonNull(item.getSize()) && nonNull(product)) {
                    productVariantRepository.findByProductIdAndSize(product.getId(), item.getSize())
                            .ifPresent(v -> {
                                v.setStockQuantity(v.getStockQuantity() + item.getQuantity());
                                productVariantRepository.save(v);
                            });
                }
                if (nonNull(product)) {
                    int restoredSales = (nonNull(product.getSalesCount()) ? product.getSalesCount() : 0) - item.getQuantity();
                    product.setSalesCount(Math.max(restoredSales, 0));
                    productRepository.save(product);
                }
            }
        }

        if (previousStatus == CANCELLED && status != CANCELLED) {
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                if (nonNull(item.getSize()) && nonNull(product)) {
                    ProductVariant variant = productVariantRepository
                            .findByProductIdAndSize(product.getId(), item.getSize())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Size " + item.getSize() + " no longer available for: " + product.getName()));
                    if (variant.getStockQuantity() < item.getQuantity()) {
                        throw new RuntimeException("Insufficient stock to reactivate order for product: "
                                + product.getName() + " (size " + item.getSize() + ")");
                    }
                    variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
                    productVariantRepository.save(variant);
                }
                if (nonNull(product)) {
                    product.setSalesCount((nonNull(product.getSalesCount()) ? product.getSalesCount() : 0) + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }
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
                .deliveryFee(order.getDeliveryFee())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(nonNull(item.getProduct()) ? item.getProduct().getId() : null)
                .productName(nonNull(item.getProductName())
                        ? item.getProductName()
                        : (nonNull(item.getProduct()) ? item.getProduct().getName() : "—"))
                .quantity(item.getQuantity())
                .priceAtPurchase(item.getPriceAtPurchase())
                .subtotal(item.getPriceAtPurchase()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .imageUrl(nonNull(item.getProduct()) ? item.getProduct().getImageUrl() : null)
                .size(item.getSize())
                .build();
    }
}
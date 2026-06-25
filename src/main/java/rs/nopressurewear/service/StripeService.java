package rs.nopressurewear.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.nopressurewear.dto.order.GuestOrderRequest;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Cart;
import rs.nopressurewear.model.Coupon;
import rs.nopressurewear.model.Order;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.CartRepository;
import rs.nopressurewear.repository.CouponRepository;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodListParams;
import rs.nopressurewear.repository.OrderRepository;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.UserRepository;

import java.nio.charset.StandardCharsets;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${app.stripe.secret-key}")
    private String secretKey;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public PaymentIntent createPaymentIntent(Long userId, String couponCode) throws StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        BigDecimal total = cart.getCartItems().stream()
                .map(item -> {
                    BigDecimal price = nonNull(item.getProduct().getDiscountPrice())
                            ? item.getProduct().getDiscountPrice()
                            : item.getProduct().getPrice();
                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(ZERO, BigDecimal::add);

        total = applyCartCoupon(total, couponCode);

        long amountInCents = toStripeAmount(total);

        String customerId = getOrCreateCustomer(user);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .putMetadata("userId", userId.toString())
                .putMetadata("couponCode", nonNull(couponCode) ? couponCode : "")
                .build();

        return PaymentIntent.create(params);
    }

    public String getOrCreateCustomer(User user) throws StripeException {
        if (nonNull(user.getStripeCustomerId())) {
            return user.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFirstName() + " " + user.getLastName())
                .build();

        Customer customer = Customer.create(params);
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);

        return customer.getId();
    }

    public List<Map<String, String>> getSavedCards(Long userId) throws StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStripeCustomerId() == null) {
            return List.of();
        }

        PaymentMethodListParams params = PaymentMethodListParams.builder()
                .setCustomer(user.getStripeCustomerId())
                .setType(PaymentMethodListParams.Type.CARD)
                .build();

        PaymentMethodCollection methods = PaymentMethod.list(params);

        return methods.getData().stream()
                .map(pm -> {
                    Map<String, String> card = new HashMap<>();
                    card.put("id", pm.getId());
                    card.put("brand", pm.getCard().getBrand());
                    card.put("last4", pm.getCard().getLast4());
                    card.put("expMonth", String.valueOf(pm.getCard().getExpMonth()));
                    card.put("expYear", String.valueOf(pm.getCard().getExpYear()));
                    return card;
                })
                .toList();
    }

    public void deleteCard(String paymentMethodId) throws StripeException {
        PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
        pm.detach();
    }

    public String createSetupIntent(Long userId) throws StripeException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String customerId = getOrCreateCustomer(user);

        com.stripe.param.SetupIntentCreateParams params = com.stripe.param.SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build();

        com.stripe.model.SetupIntent setupIntent = com.stripe.model.SetupIntent.create(params);
        return setupIntent.getClientSecret();
    }

    public PaymentIntent createGuestPaymentIntent(List<GuestOrderRequest.GuestOrderItem> items, String couponCode) throws StripeException {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("No items");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (GuestOrderRequest.GuestOrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            BigDecimal price = nonNull(product.getDiscountPrice()) ? product.getDiscountPrice() : product.getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        total = applyCartCoupon(total, couponCode);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toStripeAmount(total))
                .setCurrency("usd")
                .addPaymentMethodType("card")
                .build();

        return PaymentIntent.create(params);
    }

    private BigDecimal applyCartCoupon(BigDecimal total, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) return total;
        Coupon coupon = couponRepository.findByCode(couponCode.toUpperCase()).orElse(null);
        if (nonNull(coupon) && coupon.isActive() && coupon.getUsageCount() < coupon.getUsageLimit()) {
            return total.subtract(CouponService.applyCouponDiscount(coupon, total));
        }
        return total;
    }

    private static long toStripeAmount(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(100)).longValue();
    }

    public void handleWebhookEvent(byte[] payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Stripe webhook secret not configured — skipping event");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(
                    new String(payload, StandardCharsets.UTF_8), sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid Stripe webhook signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> event.getDataObjectDeserializer()
                    .getObject()
                    .ifPresent(obj -> confirmOrderPayment(((PaymentIntent) obj).getId(), "PAID"));
            case "payment_intent.payment_failed" -> event.getDataObjectDeserializer()
                    .getObject()
                    .ifPresent(obj -> confirmOrderPayment(((PaymentIntent) obj).getId(), "FAILED"));
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void confirmOrderPayment(String paymentIntentId, String newStatus) {
        orderRepository.findByStripePaymentId(paymentIntentId).ifPresent(order -> {
            if (!"PAID".equals(order.getPaymentStatus())) {
                order.setPaymentStatus(newStatus);
                orderRepository.save(order);
                log.info("Order {} payment status updated to {} via webhook", order.getOrderCode(), newStatus);
            }
        });
    }
}
package rs.nopressurewear.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.order.GuestOrderRequest;
import rs.nopressurewear.dto.payment.PaymentIntentRequest;
import rs.nopressurewear.dto.payment.PaymentIntentResponse;
import rs.nopressurewear.service.StripeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;

    @PostMapping("/create-payment-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @RequestBody PaymentIntentRequest request) throws StripeException {

        PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                request.getUserId(),
                request.getCouponCode()
        );

        return ResponseEntity.ok(PaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency())
                .build());
    }

    @PostMapping("/guest-payment-intent")
    public ResponseEntity<PaymentIntentResponse> createGuestPaymentIntent(
            @RequestBody Map<String, Object> request) throws StripeException {

        List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) request.get("items");
        List<GuestOrderRequest.GuestOrderItem> items = itemsRaw.stream().map(m -> {
            GuestOrderRequest.GuestOrderItem item = new GuestOrderRequest.GuestOrderItem();
            item.setProductId(Long.valueOf(m.get("productId").toString()));
            item.setQuantity(Integer.valueOf(m.get("quantity").toString()));
            return item;
        }).toList();

        String couponCode = (String) request.get("couponCode");

        PaymentIntent paymentIntent = stripeService.createGuestPaymentIntent(items, couponCode);

        return ResponseEntity.ok(PaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .amount(paymentIntent.getAmount())
                .currency(paymentIntent.getCurrency())
                .build());
    }

    @GetMapping("/cards/{userId}")
    public ResponseEntity<List<Map<String, String>>> getSavedCards(@PathVariable Long userId)
            throws StripeException {
        return ResponseEntity.ok(stripeService.getSavedCards(userId));
    }

    @PostMapping("/setup-intent/{userId}")
    public ResponseEntity<Map<String, String>> createSetupIntent(@PathVariable Long userId)
            throws StripeException {
        String clientSecret = stripeService.createSetupIntent(userId);
        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    @DeleteMapping("/cards/{paymentMethodId}")
    public ResponseEntity<Void> deleteCard(@PathVariable String paymentMethodId)
            throws StripeException {
        stripeService.deleteCard(paymentMethodId);
        return ResponseEntity.noContent().build();
    }
}
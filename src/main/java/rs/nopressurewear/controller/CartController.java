package rs.nopressurewear.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.cart.CartItemRequest;
import rs.nopressurewear.dto.cart.CartResponse;
import rs.nopressurewear.service.CartService;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable Long userId,
                                                @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @PutMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable Long userId,
                                                   @PathVariable Long cartItemId,
                                                   @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(userId, cartItemId, request));
    }

    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long userId,
                                                   @PathVariable Long cartItemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, cartItemId));
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
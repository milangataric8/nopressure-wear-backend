package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.dto.cart.CartItemRequest;
import rs.nopressurewear.dto.cart.CartItemResponse;
import rs.nopressurewear.dto.cart.CartResponse;
import rs.nopressurewear.exception.FieldValidationException;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.*;
import rs.nopressurewear.repository.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toResponse(cart);
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new RuntimeException("Product is not available");
        }

        if (request.getSize() == null) {
            throw new RuntimeException("Size is required");
        }

        ProductVariant variant = productVariantRepository
                .findByProductIdAndSize(product.getId(), request.getSize())
                .orElseThrow(() -> new ResourceNotFoundException("Size " + request.getSize() + " is not available for this product"));

        if (variant.getStockQuantity() < request.getQuantity()) {
            throw new FieldValidationException("validation.outOfStock", "size");
        }

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductIdAndSize(cart.getId(), product.getId(), request.getSize());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .size(request.getSize())
                    .build();
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        touchCart(cart);
        return toResponse(cartRepository.save(cart));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to this cart");
        }

        if (request.getQuantity() == 0) {
            cart.getCartItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        touchCart(cart);
        return toResponse(cartRepository.save(cart));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Cart item does not belong to this cart");
        }

        cart.getCartItems().remove(item);
        cartItemRepository.delete(item);

        touchCart(cart);
        return toResponse(cartRepository.save(cart));
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Cart newCart = Cart.builder()
                    .user(user)
                    .updatedAt(LocalDateTime.now())
                    .build();

            return cartRepository.save(newCart);
        });
    }

    private void touchCart(Cart cart) {
        cart.setUpdatedAt(LocalDateTime.now());
        cart.setReminderSentAt(null);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getCartItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .totalAmount(total)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        BigDecimal subtotal = nonNull(item.getProduct().getDiscountPrice())
                ? item.getProduct().getDiscountPrice()
                : item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .productPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .imageUrl(item.getProduct().getImageUrl())
                .subtotal(subtotal)
                .discountPrice(item.getProduct().getDiscountPrice())
                .discountPercentage(item.getProduct().getDiscountPercentage())
                .size(item.getSize())
                .build();
    }
}
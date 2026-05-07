package rs.nopressure.wear.webshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressure.wear.webshop.dto.cart.CartItemRequest;
import rs.nopressure.wear.webshop.dto.cart.CartItemResponse;
import rs.nopressure.wear.webshop.dto.cart.CartResponse;
import rs.nopressure.wear.webshop.exception.ResourceNotFoundException;
import rs.nopressure.wear.webshop.model.Cart;
import rs.nopressure.wear.webshop.model.CartItem;
import rs.nopressure.wear.webshop.model.Product;
import rs.nopressure.wear.webshop.model.User;
import rs.nopressure.wear.webshop.repository.CartItemRepository;
import rs.nopressure.wear.webshop.repository.CartRepository;
import rs.nopressure.wear.webshop.repository.ProductRepository;
import rs.nopressure.wear.webshop.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.math.BigDecimal.ZERO;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

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

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock");
        }

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem);
        }

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
                    .build();
            return cartRepository.save(newCart);
        });
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
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productSku(item.getProduct().getSku())
                .productPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
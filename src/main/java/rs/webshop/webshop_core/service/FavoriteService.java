package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.webshop.webshop_core.dto.favorite.FavoriteResponse;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Favorite;
import rs.webshop.webshop_core.model.Product;
import rs.webshop.webshop_core.model.User;
import rs.webshop.webshop_core.repository.FavoriteRepository;
import rs.webshop.webshop_core.repository.ProductRepository;
import rs.webshop.webshop_core.repository.UserRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public List<FavoriteResponse> getUserFavorites(Long userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public int getCount(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

    public boolean isFavorite(Long userId, Long productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public Map<String, Object> toggle(Long userId, Long productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            favoriteRepository.deleteByUserIdAndProductId(userId, productId);
            return Map.of(
                    "favorited", false,
                    "count", favoriteRepository.countByUserId(userId)
            );
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            Favorite favorite = Favorite.builder()
                    .user(user)
                    .product(product)
                    .build();
            favoriteRepository.save(favorite);

            return Map.of(
                    "favorited", true,
                    "count", favoriteRepository.countByUserId(userId)
            );
        }
    }

    private FavoriteResponse toResponse(Favorite favorite) {
        Product product = favorite.getProduct();
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .productPrice(product.getPrice())
                .productInStock(product.getStockQuantity() > 0)
                .createdAt(favorite.getCreatedAt())
                .productDiscountPrice(product.getDiscountPrice())
                .productDiscountPercentage(product.getDiscountPercentage())
                .build();
    }
}
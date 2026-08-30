package rs.nopressurewear.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.dto.favorite.FavoriteResponse;
import rs.nopressurewear.exception.ResourceNotFoundException;
import rs.nopressurewear.model.Favorite;
import rs.nopressurewear.model.Product;
import rs.nopressurewear.model.User;
import rs.nopressurewear.repository.FavoriteRepository;
import rs.nopressurewear.repository.ProductRepository;
import rs.nopressurewear.repository.ProductVariantRepository;
import rs.nopressurewear.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public List<FavoriteResponse> getUserFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Set<Long> productIds = favorites.stream()
                .map(f -> f.getProduct().getId())
                .collect(Collectors.toSet());

        Set<Long> inStockProductIds = productIds.isEmpty()
                ? Set.of()
                : productVariantRepository.sumStockByProductIds(productIds).stream()
                        .filter(row -> ((Number) row[1]).longValue() > 0)
                        .map(row -> ((Number) row[0]).longValue())
                        .collect(Collectors.toSet());

        return favorites.stream()
                .map(f -> toResponse(f, inStockProductIds))
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

    private FavoriteResponse toResponse(Favorite favorite, Set<Long> inStockProductIds) {
        Product product = favorite.getProduct();
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .productPrice(product.getPrice())
                .productInStock(inStockProductIds.contains(product.getId()))
                .createdAt(favorite.getCreatedAt())
                .productDiscountPrice(product.getDiscountPrice())
                .productDiscountPercentage(product.getDiscountPercentage())
                .build();
    }
}
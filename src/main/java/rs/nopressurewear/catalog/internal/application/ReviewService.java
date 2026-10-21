package rs.nopressurewear.catalog.internal.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.nopressurewear.catalog.api.dto.review.ReviewRequest;
import rs.nopressurewear.catalog.api.dto.review.ReviewResponse;
import rs.nopressurewear.platform.error.DuplicateResourceException;
import rs.nopressurewear.platform.error.ResourceNotFoundException;
import rs.nopressurewear.catalog.internal.domain.Product;
import rs.nopressurewear.catalog.internal.domain.ProductReview;
import rs.nopressurewear.identity.internal.domain.User;
import rs.nopressurewear.catalog.internal.persistence.ProductRepository;
import rs.nopressurewear.catalog.internal.persistence.ProductReviewRepository;
import rs.nopressurewear.identity.internal.persistence.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<ReviewResponse> getReviewsForProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse addReview(Long productId, Long userId, ReviewRequest request) {
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new DuplicateResourceException("errors.review.alreadyReviewed");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);
        updateProductRating(product);

        return toResponse(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own review");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        updateProductRating(product);
    }

    private void updateProductRating(Product product) {
        Double avg = reviewRepository.findAverageRatingByProductId(product.getId());
        Integer count = reviewRepository.countByProductId(product.getId());

        product.setAverageRating(nonNull(avg)
                ? BigDecimal.valueOf(avg).setScale(1, HALF_UP)
                : ZERO);
        product.setRatingCount(nonNull(count) ? count : 0);
        productRepository.save(product);
    }

    private ReviewResponse toResponse(ProductReview review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
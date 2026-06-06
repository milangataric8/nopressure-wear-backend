package rs.webshop.webshop_core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.webshop.webshop_core.dto.review.ReviewRequest;
import rs.webshop.webshop_core.dto.review.ReviewResponse;
import rs.webshop.webshop_core.exception.DuplicateResourceException;
import rs.webshop.webshop_core.exception.ResourceNotFoundException;
import rs.webshop.webshop_core.model.Product;
import rs.webshop.webshop_core.model.ProductReview;
import rs.webshop.webshop_core.model.User;
import rs.webshop.webshop_core.repository.ProductRepository;
import rs.webshop.webshop_core.repository.ProductReviewRepository;
import rs.webshop.webshop_core.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
            throw new DuplicateResourceException("You already reviewed this product");
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

        product.setAverageRating(avg != null
                ? BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        product.setRatingCount(count != null ? count : 0);
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
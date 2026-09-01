package rs.nopressurewear.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.nopressurewear.dto.review.ReviewRequest;
import rs.nopressurewear.dto.review.ReviewResponse;
import rs.nopressurewear.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsForProduct(productId));
    }

    @PostMapping("/product/{productId}/user/{userId}")
    public ResponseEntity<ReviewResponse> addReview(
            @PathVariable Long productId,
            @PathVariable Long userId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.addReview(productId, userId, request));
    }

    @DeleteMapping("/{reviewId}/user/{userId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @PathVariable Long userId) {
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }
}
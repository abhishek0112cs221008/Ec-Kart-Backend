package com.ecommerce.ecommercebackend.Review.controller;

import com.ecommerce.ecommercebackend.Review.dto.ReviewRequest;
import com.ecommerce.ecommercebackend.Review.dto.ReviewResponse;
import com.ecommerce.ecommercebackend.Review.service.ReviewService;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{productId}")
    public ResponseEntity<?> postReview(
            @PathVariable UUID productId,
            @RequestBody ReviewRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new MessageResponse("You must be logged in to post a review."));
        }

        try {
            request.setProductId(productId);
            ReviewResponse response = reviewService.postReview(authentication.getName(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(
            @PathVariable UUID id,
            @RequestBody ReviewRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(new MessageResponse("You must be logged in to edit a review."));
        }

        try {
            ReviewResponse response = reviewService.updateReview(id, authentication.getName(), request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/{productId}/rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.getAverageRating(productId));
    }
}

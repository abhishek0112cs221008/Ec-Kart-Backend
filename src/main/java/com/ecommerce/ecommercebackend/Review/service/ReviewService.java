package com.ecommerce.ecommercebackend.Review.service;

import com.ecommerce.ecommercebackend.Review.dto.ReviewRequest;
import com.ecommerce.ecommercebackend.Review.dto.ReviewResponse;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponse postReview(String userEmail, ReviewRequest request);
    ReviewResponse updateReview(UUID id, String userEmail, ReviewRequest request);
    List<ReviewResponse> getReviewsByProduct(UUID productId);
    Double getAverageRating(UUID productId);
    Long getReviewCount(UUID productId);
    Double getSellerAverageRating(UUID sellerId);
}

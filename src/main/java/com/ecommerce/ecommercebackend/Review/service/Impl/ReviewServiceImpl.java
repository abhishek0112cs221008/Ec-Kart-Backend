package com.ecommerce.ecommercebackend.Review.service.Impl;

import com.ecommerce.ecommercebackend.Product.entity.Product;
import com.ecommerce.ecommercebackend.Product.repository.ProductRepository;
import com.ecommerce.ecommercebackend.Review.dto.ReviewRequest;
import com.ecommerce.ecommercebackend.Review.dto.ReviewResponse;
import com.ecommerce.ecommercebackend.Review.entity.Review;
import com.ecommerce.ecommercebackend.Review.repository.ReviewRepository;
import com.ecommerce.ecommercebackend.Review.service.ReviewService;
import com.ecommerce.ecommercebackend.auth.exception.UserNotFoundException;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UsersRepo userRepository;

    @Override
    @Transactional
    public ReviewResponse postReview(String userEmail, ReviewRequest request) {
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        // Check if user already reviewed this product
        if (reviewRepository.countByUserIdAndProductId(user.getId(), product.getId()) > 0) {
            throw new RuntimeException("You have already reviewed this product.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        return mapToResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID id, String userEmail, ReviewRequest request) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + id));

        if (!review.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("You are not authorized to edit this review.");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updated = reviewRepository.save(review);
        return mapToResponse(updated);
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(UUID productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAverageRating(UUID productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public Long getReviewCount(UUID productId) {
        return reviewRepository.countByProductId(productId);
    }

    @Override
    public Double getSellerAverageRating(UUID sellerId) {
        Double avg = reviewRepository.findAverageRatingBySellerId(sellerId);
        return avg != null ? avg : 0.0;
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .userFullName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .rating(review.getRating())
                .comment(review.getComment())
                .userEmail(review.getUser().getEmail())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

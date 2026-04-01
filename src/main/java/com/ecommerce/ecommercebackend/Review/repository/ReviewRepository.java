package com.ecommerce.ecommercebackend.Review.repository;

import com.ecommerce.ecommercebackend.Review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByProductId(UUID productId);

    List<Review> findByProductIdOrderByCreatedAtDesc(UUID productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") UUID productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId")
    Long countByProductId(@Param("productId") UUID productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.seller.id = :sellerId")
    Double findAverageRatingBySellerId(@Param("sellerId") UUID sellerId);

    long countByUserIdAndProductId(UUID userId, UUID productId);
}

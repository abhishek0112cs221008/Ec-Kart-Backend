package com.ecommerce.ecommercebackend.Review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID userId;
    private String userFullName;
    private int rating;
    private String comment;
    private String userEmail;
    private LocalDateTime createdAt;
}

package com.ecommerce.ecommercebackend.Product.dto;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private String message;
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Integer stock;
    private Boolean active;
    private String sellerName;
    private String sellerEmail;
    private UUID sellerId;
    private Double averageRating;
    private Long reviewCount;
    private Double sellerRating;
    private BigDecimal floorPrice;
    private String targetGroup;
}


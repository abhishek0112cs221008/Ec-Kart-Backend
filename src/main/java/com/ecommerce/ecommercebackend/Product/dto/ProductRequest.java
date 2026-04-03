package com.ecommerce.ecommercebackend.Product.dto;

import lombok.*;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductRequest {
    private String name;
    private String description;
    private Double price;
    private Double floorPrice;
    private Long categoryId;
    private Integer stock;
    private String targetGroup;

}


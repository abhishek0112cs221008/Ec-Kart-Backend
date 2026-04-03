package com.ecommerce.ecommercebackend.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileResponseDTO {
    private Long id;
    private String storeName;
    private String bio;
    private String contactEmail;
    private String contactPhone;
    private String logoUrl;
    private LocalDateTime createdAt;
}

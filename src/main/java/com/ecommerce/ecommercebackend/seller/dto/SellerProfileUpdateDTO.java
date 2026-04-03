package com.ecommerce.ecommercebackend.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileUpdateDTO {
    private String storeName;
    private String bio;
    private String contactEmail;
    private String contactPhone;
}

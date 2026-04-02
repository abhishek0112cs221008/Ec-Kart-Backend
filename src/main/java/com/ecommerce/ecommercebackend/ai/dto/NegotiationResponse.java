package com.ecommerce.ecommercebackend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationResponse {
    private String response;
    private boolean accepted;
    private Double finalPrice;
}

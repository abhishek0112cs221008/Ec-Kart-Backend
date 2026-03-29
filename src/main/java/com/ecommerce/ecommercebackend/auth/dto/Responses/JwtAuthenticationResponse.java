package com.ecommerce.ecommercebackend.auth.dto.Responses;

import lombok.Builder;
import lombok.Data;

@Data

public class JwtAuthenticationResponse {
    private String token;
    private String refreshToken;
}


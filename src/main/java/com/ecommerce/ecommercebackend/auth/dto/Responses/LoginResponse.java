package com.ecommerce.ecommercebackend.auth.dto.Responses;

import com.ecommerce.ecommercebackend.entity.Role;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private UUID id;
    private String message;
    private String accessToken; // JWT token
    private String refreshToken;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private Role role;
    private boolean sellerVerified;
}


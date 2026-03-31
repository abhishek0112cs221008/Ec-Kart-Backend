package com.ecommerce.ecommercebackend.auth.dto.Responses;

import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileResponse {
    private UUID id;
    private String message;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String phoneNumber;
}


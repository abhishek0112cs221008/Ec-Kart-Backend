package com.ecommerce.ecommercebackend.auth.dto.Responses;

import com.ecommerce.ecommercebackend.entity.Role;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetProfileResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private Role role;
    private boolean sellerVerified;
    private String phoneNumber;
}



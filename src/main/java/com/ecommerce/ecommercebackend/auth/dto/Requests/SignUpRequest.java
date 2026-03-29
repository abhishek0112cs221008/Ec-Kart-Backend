package com.ecommerce.ecommercebackend.auth.dto.Requests;


import com.ecommerce.ecommercebackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SignUpRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;
//    private MultipartFile profileImage;
}


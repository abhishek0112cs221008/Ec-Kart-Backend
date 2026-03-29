package com.ecommerce.ecommercebackend;

import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsersRepo usersRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@gmail.com";
        if (usersRepo.findByEmail(adminEmail).isEmpty()) {
            Users admin = Users.builder()
                    .firstName("Platform")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .emailVerified(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            usersRepo.save(admin);
            System.out.println("Default Admin account seeded: " + adminEmail);
        }
    }
}

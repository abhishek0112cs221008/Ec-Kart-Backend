package com.ecommerce.ecommercebackend.repository;


import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepo extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(Users user);
    void deleteByUser(Users user);
}


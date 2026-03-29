package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.OTP;
import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByUserAndOtp(Users user, String otp);
    void deleteByUser(Users user);
    void deleteByExpiryDateBefore(LocalDateTime time);

}


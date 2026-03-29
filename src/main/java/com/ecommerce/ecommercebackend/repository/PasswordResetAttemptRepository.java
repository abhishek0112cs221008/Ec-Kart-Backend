package com.ecommerce.ecommercebackend.repository;


import com.ecommerce.ecommercebackend.entity.PasswordResetAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetAttemptRepository extends JpaRepository<PasswordResetAttempt, Long> {
    Optional<PasswordResetAttempt> findByEmail(String email);
}


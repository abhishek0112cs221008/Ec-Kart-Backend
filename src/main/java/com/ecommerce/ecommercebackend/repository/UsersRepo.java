package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ecommerce.ecommercebackend.entity.Role;

@Repository
public interface UsersRepo extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);
    List<Users> findByRoleAndSellerVerifiedFalse(Role role);
    List<Users> findByEmailVerifiedFalseAndCreatedAtBefore(java.time.LocalDateTime timestamp);
}


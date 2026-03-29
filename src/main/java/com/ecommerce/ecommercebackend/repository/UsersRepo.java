package com.ecommerce.ecommercebackend.repository;

import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsersRepo extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);
    // Users findByRole(Role role);
}

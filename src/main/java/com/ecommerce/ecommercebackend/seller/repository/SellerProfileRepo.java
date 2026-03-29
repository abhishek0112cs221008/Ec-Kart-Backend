package com.ecommerce.ecommercebackend.seller.repository;

import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.seller.entity.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@SuppressWarnings("ALL")
public interface SellerProfileRepo extends JpaRepository<SellerProfile, Long> {
    Optional<SellerProfile> findByUser(Users user);
}

package com.ecommerce.ecommercebackend.Wishlist.repository;

import com.ecommerce.ecommercebackend.Wishlist.entity.Wishlist;
import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    Optional<Wishlist> findByUser(Users user);
}

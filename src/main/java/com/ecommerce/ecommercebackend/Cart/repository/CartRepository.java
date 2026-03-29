package com.ecommerce.ecommercebackend.Cart.repository;

import com.ecommerce.ecommercebackend.Cart.entity.Cart;
import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUser(Users user);
}

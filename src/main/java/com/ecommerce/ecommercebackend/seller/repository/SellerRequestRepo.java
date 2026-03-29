package com.ecommerce.ecommercebackend.seller.repository;

import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.seller.entity.SellerRequest;
import com.ecommerce.ecommercebackend.seller.entity.SellerRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

@SuppressWarnings("ALL")
public interface SellerRequestRepo extends JpaRepository<SellerRequest, Long> {
    Optional<SellerRequest> findByUser(Users user);

    List<SellerRequest> findAllByStatus(SellerRequestStatus sellerRequestStatus);
}

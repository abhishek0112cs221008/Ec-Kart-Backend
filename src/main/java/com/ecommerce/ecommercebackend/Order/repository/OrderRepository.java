package com.ecommerce.ecommercebackend.Order.repository;


import com.ecommerce.ecommercebackend.Order.entity.Order;
import com.ecommerce.ecommercebackend.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByUser(Users user, Pageable pageable);
    Optional<Order> findById(Long id);
}

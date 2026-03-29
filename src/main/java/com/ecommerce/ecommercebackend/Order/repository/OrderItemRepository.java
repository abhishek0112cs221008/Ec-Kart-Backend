package com.ecommerce.ecommercebackend.Order.repository;


import com.ecommerce.ecommercebackend.Order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> { }


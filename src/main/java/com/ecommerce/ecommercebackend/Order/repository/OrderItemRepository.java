package com.ecommerce.ecommercebackend.Order.repository;

import com.ecommerce.ecommercebackend.Order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.seller.email = :email ORDER BY oi.order.createdAt DESC")
    List<OrderItem> findAllBySellerEmail(@Param("email") String email);

    @Query("SELECT SUM(oi.priceAtPurchase * oi.quantity) FROM OrderItem oi WHERE oi.product.seller.email = :email")
    BigDecimal calculateTotalRevenueBySellerEmail(@Param("email") String email);

    @Query("SELECT COUNT(DISTINCT oi.order.id) FROM OrderItem oi WHERE oi.product.seller.email = :email")
    long countOrdersBySellerEmail(@Param("email") String email);
}


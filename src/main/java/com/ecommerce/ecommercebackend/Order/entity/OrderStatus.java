package com.ecommerce.ecommercebackend.Order.entity;


public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    DELIVERED,
    PAID,
    CANCELLED,
    FAILED,
    REFUNDED
}


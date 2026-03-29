package com.ecommerce.ecommercebackend.Order.exception;


public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message , Long orderId) {
        super(message + orderId);
    }
}


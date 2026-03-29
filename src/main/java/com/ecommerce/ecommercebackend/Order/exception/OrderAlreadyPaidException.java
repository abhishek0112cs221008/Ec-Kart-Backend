package com.ecommerce.ecommercebackend.Order.exception;

public class OrderAlreadyPaidException extends RuntimeException {
    public OrderAlreadyPaidException(Long orderId) {
        super("Order already paid: " + orderId);
    }
}

package com.ecommerce.ecommercebackend.payment.exception;

public class PaymentNotCompletedException extends RuntimeException {
    public PaymentNotCompletedException(String message) {
        super(message);
    }
}


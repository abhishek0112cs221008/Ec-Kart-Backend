package com.ecommerce.ecommercebackend.payment.exception;

public class StripeOperationException extends RuntimeException {
    public StripeOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}


package com.ecommerce.ecommercebackend.payment.dto;

import lombok.*;

/**
 * Request DTO for confirming a Razorpay payment.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentConfirmRequest {
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;
}

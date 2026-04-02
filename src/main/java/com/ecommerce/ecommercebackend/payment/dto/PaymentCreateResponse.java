package com.ecommerce.ecommercebackend.payment.dto;

import lombok.*;

/**
 * Response returned when creating a Razorpay Order.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentCreateResponse {
    private String razorpayOrderId;
    private Long amount; // in paise
    private String currency;
    private String keyId;
    private String name;
    private String email;
    private String contact;

    public static PaymentCreateResponse of(String razorpayOrderId, Long amount, String currency, String keyId, String name, String email, String contact) {
        return new PaymentCreateResponse(razorpayOrderId, amount, currency, keyId, name, email, contact);
    }
}

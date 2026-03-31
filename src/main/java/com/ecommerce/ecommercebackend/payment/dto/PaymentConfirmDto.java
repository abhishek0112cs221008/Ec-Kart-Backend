package com.ecommerce.ecommercebackend.payment.dto;


import lombok.*;

/**
 * Response returned when confirming a session/payment.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentConfirmDto {
    private String status;
    private String message;
    private boolean paid;

    public static PaymentConfirmDto ok(String message) {
        return new PaymentConfirmDto("OK", message, true);
    }

    public static PaymentConfirmDto pending(String message) {
        return new PaymentConfirmDto("PENDING", message, false);
    }
}

package com.ecommerce.ecommercebackend.payment.Controller;

import com.ecommerce.ecommercebackend.payment.dto.*;
import com.ecommerce.ecommercebackend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create a Razorpay order for a specific ecommerce order.
     */
    @PostMapping("/create/{orderId}")
    public PaymentCreateResponse createPayment(
            @PathVariable Long orderId,
            Authentication authentication) {

        return paymentService.createCheckoutSessionForOrder(
                orderId,
                authentication.getName()
        );
    }

    /**
     * Confirm the payment status of a Razorpay order via signature verification.
     */
    @PostMapping("/confirm")
    public PaymentConfirmDto confirmPayment(
            @RequestBody PaymentConfirmRequest request,
            Authentication authentication) {

        return paymentService.confirmPayment(
                request,
                authentication.getName()
        );
    }

    /**
     * Process a refund for a paid order via Razorpay.
     */
    @PostMapping("/refund")
    public ResponseEntity<?> refund(@RequestBody RefundRequest req, Authentication auth) {
        Object refund = paymentService.refundPaymentForOrder(req, auth.getName());
        return ResponseEntity.ok(refund);
    }
}

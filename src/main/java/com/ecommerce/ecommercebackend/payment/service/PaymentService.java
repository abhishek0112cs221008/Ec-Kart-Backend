package com.ecommerce.ecommercebackend.payment.service;
import com.ecommerce.ecommercebackend.payment.dto.*;
import com.stripe.model.Refund;



public interface PaymentService {
    PaymentCreateResponse createCheckoutSessionForOrder(Long orderId, String userEmail);
    PaymentConfirmDto confirmPaymentBySessionId(String sessionId, String userEmail);
    Refund refundPaymentForOrder(RefundRequest request, String userEmail);
}


package com.ecommerce.ecommercebackend.payment.service;

import com.ecommerce.ecommercebackend.payment.dto.*;

public interface PaymentService {
    PaymentCreateResponse createCheckoutSessionForOrder(Long orderId, String userEmail);
    PaymentConfirmDto confirmPayment(PaymentConfirmRequest request, String userEmail);
    Object refundPaymentForOrder(RefundRequest request, String userEmail);
}

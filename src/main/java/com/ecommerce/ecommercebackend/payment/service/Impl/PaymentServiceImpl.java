package com.ecommerce.ecommercebackend.payment.service.Impl;

import com.ecommerce.ecommercebackend.Order.entity.Order;
import com.ecommerce.ecommercebackend.Order.entity.OrderStatus;
import com.ecommerce.ecommercebackend.Order.repository.OrderRepository;
import com.ecommerce.ecommercebackend.payment.dto.*;
import com.ecommerce.ecommercebackend.payment.entity.Payment;
import com.ecommerce.ecommercebackend.payment.exception.OrderPaymentNotAllowedException;
import com.ecommerce.ecommercebackend.payment.exception.PaymentNotFoundException;
import com.ecommerce.ecommercebackend.payment.repository.PaymentRepository;
import com.ecommerce.ecommercebackend.payment.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.keyId}")
    private String razorpayKeyId;

    @Value("${razorpay.keySecret}")
    private String razorpayKeySecret;

    @Override
    public PaymentCreateResponse createCheckoutSessionForOrder(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderPaymentNotAllowedException("Order not found: " + orderId));

        if (order.getUser() == null || !order.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new OrderPaymentNotAllowedException("Order does not belong to authenticated user");
        }

        try {
            // Amount in paise (multiply by 100)
            BigDecimal amountInPaise = order.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise.longValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + orderId);
            orderRequest.put("payment_capture", 1); // Auto capture

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            Payment payment = new Payment(order.getId(), razorpayOrder.get("id"));
            payment.setStatus(Payment.Status.CREATED);
            payment.setCreatedAt(OffsetDateTime.now());
            payment.setExpiresAt(payment.getCreatedAt().plusMinutes(60));
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(order);

            return PaymentCreateResponse.of(
                    razorpayOrder.get("id"),
                    amountInPaise.longValue(),
                    "INR",
                    razorpayKeyId,
                    order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                    order.getUser().getEmail(),
                    order.getUser().getPhoneNumber()
            );

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed", e);
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentConfirmDto confirmPayment(PaymentConfirmRequest request, String userEmail) {
        try {
            // Verify signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (!isValid) {
                return PaymentConfirmDto.pending("Invalid payment signature");
            }

            Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + request.getRazorpayOrderId()));

            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new OrderPaymentNotAllowedException("Order not found"));

            if (!order.getUser().getEmail().equalsIgnoreCase(userEmail)) {
                throw new OrderPaymentNotAllowedException("User mismatch");
            }

            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus(Payment.Status.PAID);
            payment.setPaidAt(OffsetDateTime.now());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            return PaymentConfirmDto.ok("Payment successful and verified");

        } catch (Exception e) {
            log.error("Razorpay confirmation error", e);
            return PaymentConfirmDto.pending("Verification failed: " + e.getMessage());
        }
    }

    @Override
    public Object refundPaymentForOrder(RefundRequest request, String userEmail) {
        Payment payment = paymentRepository.findByOrderIdAndStatus(request.getOrderId(), Payment.Status.PAID)
                .orElseThrow(() -> new PaymentNotFoundException("Paid payment not found for order id=" + request.getOrderId()));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderPaymentNotAllowedException("Order not found"));

        if (!order.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new OrderPaymentNotAllowedException("User cannot refund this order");
        }

        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("payment_id", payment.getRazorpayPaymentId());
            
            if (request.getAmount() != null) {
                BigDecimal amountInPaise = request.getAmount().multiply(new BigDecimal(100));
                refundRequest.put("amount", amountInPaise.longValue());
            }

            com.razorpay.Refund razorpayRefund = razorpayClient.payments.refund(refundRequest);

            payment.setStatus(Payment.Status.REFUNDED);
            payment.setRefundedAt(OffsetDateTime.now());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);

            Map<String, Object> response = new HashMap<>();
            response.put("refundId", razorpayRefund.get("id"));
            response.put("status", razorpayRefund.get("status"));
            return response;

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed", e);
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }
}

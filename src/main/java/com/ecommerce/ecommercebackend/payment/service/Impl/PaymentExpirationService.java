package com.ecommerce.ecommercebackend.payment.service.Impl;

import com.ecommerce.ecommercebackend.payment.entity.Payment;
import com.ecommerce.ecommercebackend.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Check for expired payments every minute.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentExpirationService {

    private final PaymentRepository paymentRepository;
    private final com.ecommerce.ecommercebackend.Order.service.OrderService orderService;
    private final RazorpayClient razorpayClient;

    @Value("${payment.session.ttl.minutes:60}")
    private long ttlMinutes;

    @Scheduled(cron = "${payment.expire-check-cron}")
    public void expireOldPayments() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(ttlMinutes);
        List<Payment> toExpire = paymentRepository.findByStatusAndCreatedAtBefore(Payment.Status.CREATED, cutoff);

        for (Payment p : toExpire) {
            try {
                // Check with Razorpay
                Order razorpayOrder = razorpayClient.orders.fetch(p.getRazorpayOrderId());
                String status = razorpayOrder.get("status");

                if ("paid".equalsIgnoreCase(status)) {
                    p.setStatus(Payment.Status.PAID);
                    p.setPaidAt(OffsetDateTime.now());
                    paymentRepository.save(p);
                    continue;
                }

                // If expired or still created after TTL
                p.setStatus(Payment.Status.FAILED);
                paymentRepository.save(p);

                // Restore stock
                restoreStockForOrder(p.getOrderId());

            } catch (Exception ex) {
                log.error("Error expiring payment {}", p.getRazorpayOrderId(), ex);
            }
        }
    }

    private void restoreStockForOrder(Long orderId) {
        try {
            orderService.cancelOrderSystem(orderId);
        } catch (Exception e) {
            log.error("Failed to cancel order system {}", orderId, e);
        }
    }
}

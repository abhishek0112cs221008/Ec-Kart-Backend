package com.ecommerce.ecommercebackend.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payments")
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Column(unique = true)
    private String razorpayOrderId;

    private Long platformFeeAmount;       // paise
    private Long sellerAmount;            // paise
    private String sellerRazorpayAccountId;

    public enum Status { CREATED, PAID, FAILED, REFUNDED }

    @Column(name = "razorpay_payment_id" , unique = true)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Status status = Status.CREATED;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime paidAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime refundedAt;

    public Payment(Long orderId, String razorpayOrderId) {
        this.orderId = orderId;
        this.razorpayOrderId = razorpayOrderId;
        this.createdAt = OffsetDateTime.now();
    }
}

package com.ecommerce.ecommercebackend.ai.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ecommerce.ecommercebackend.Product.entity.Product;
import com.ecommerce.ecommercebackend.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "negotiated_offers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiatedOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnoreProperties({"password", "role", "enabled", "emailVerified", "verificationCode", "resetPasswordToken"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @JsonIgnoreProperties({"category", "seller", "description"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal negotiatedPrice;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

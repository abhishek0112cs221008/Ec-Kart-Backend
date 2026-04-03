package com.ecommerce.ecommercebackend.seller.entity;

import com.ecommerce.ecommercebackend.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfile {
    @Id @GeneratedValue private Long id;

    @OneToOne(optional = false)
    private Users user;

    private String storeName;

    private String bio;

    private String contactEmail;

    private String contactPhone;

    private String logoUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

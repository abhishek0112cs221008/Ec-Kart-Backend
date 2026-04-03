package com.ecommerce.ecommercebackend.seller.dto;

import com.ecommerce.ecommercebackend.Order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Simplified DTO for the Seller Manage Orders tab.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerOrderResponse {
    private Long orderId;
    private String productTitle;
    private String customerName;
    private String shippingAddress;
    private Integer quantity;
    private BigDecimal price; // specifically for the seller's part
    private OrderStatus status;
    private LocalDateTime createdAt;
}

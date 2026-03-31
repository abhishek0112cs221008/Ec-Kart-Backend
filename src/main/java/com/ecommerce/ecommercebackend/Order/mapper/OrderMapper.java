package com.ecommerce.ecommercebackend.Order.mapper;


import com.ecommerce.ecommercebackend.Order.dto.*;
import com.ecommerce.ecommercebackend.Order.entity.*;
import java.util.stream.Collectors;

public final class OrderMapper {
    private OrderMapper() {}

    public static OrderResponse toDto(Order o) {
        OrderResponse dto = new OrderResponse();
        dto.setId(o.getId());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setStatus(o.getStatus());
        dto.setShippingAddress(o.getShippingAddress());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());
        dto.setItems(o.getItems().stream().map(OrderMapper::toItemDto).collect(Collectors.toList()));
        return dto;
    }

    public static OrderSummaryResponse toSummaryDto(Order o) {
        OrderSummaryResponse s = new OrderSummaryResponse();
        s.setId(o.getId());
        s.setTotalAmount(o.getTotalAmount());
        s.setStatus(o.getStatus());
        s.setCreatedAt(o.getCreatedAt());
        s.setItemThumbnails(o.getItems().stream()
                .map(i -> i.getProduct().getImageUrl())
                .collect(Collectors.toList()));
        return s;
    }

    public static OrderItemResponse toItemDto(OrderItem i) {
        OrderItemResponse d = new OrderItemResponse();
        d.setId(i.getId());
        d.setProductId(i.getProduct().getId());
        d.setProductName(i.getProduct().getName());
        d.setQuantity(i.getQuantity());
        d.setPriceAtPurchase(i.getPriceAtPurchase());
        d.setImageUrl(i.getProduct().getImageUrl());
        return d;
    }
}


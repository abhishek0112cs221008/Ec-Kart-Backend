package com.ecommerce.ecommercebackend.Cart.service;

import com.ecommerce.ecommercebackend.Cart.dto.*;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;

import java.util.UUID;

public interface CartService {
    CartResponse addToCart(String userEmail, UUID productId, Integer quantity);

    CartResponse updateQuantity(String userEmail, UUID productId, Integer quantity);

    CartResponse removeFromCart(String userEmail, UUID productId);

    CartResponse getCart(String userEmail);

    MessageResponse clearCart(String userEmail);
}


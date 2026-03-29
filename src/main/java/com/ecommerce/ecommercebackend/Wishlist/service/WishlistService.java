package com.ecommerce.ecommercebackend.Wishlist.service;
import com.ecommerce.ecommercebackend.Wishlist.dto.WishlistResponse;

import java.util.UUID;

public interface WishlistService {
    WishlistResponse addToWishlist(String userEmail, UUID productId);
    WishlistResponse removeFromWishlist(String userEmail, UUID productId);
    WishlistResponse getWishlist(String userEmail);
    WishlistResponse clearWishlist(String userEmail);
}


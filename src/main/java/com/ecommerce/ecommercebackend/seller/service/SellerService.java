package com.ecommerce.ecommercebackend.seller.service;

import com.ecommerce.ecommercebackend.seller.dto.SellerRequestResponse;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ecommerce.ecommercebackend.seller.dto.SellerDashboardStatsDTO;
import com.ecommerce.ecommercebackend.seller.dto.SellerOrderResponse;
import com.ecommerce.ecommercebackend.Order.entity.OrderStatus;
import com.ecommerce.ecommercebackend.seller.dto.SellerProfileResponseDTO;
import com.ecommerce.ecommercebackend.seller.dto.SellerProfileUpdateDTO;

import java.io.IOException;
import java.util.List;

public interface SellerService {

    SellerRequestResponse requestSeller(String userEmail, String storeName,String reason , MultipartFile document) throws IOException;

    MessageResponse approveRequest(Long requestId, String adminEmail);

    MessageResponse rejectRequest(Long requestId, String adminEmail, String reason);

    List<SellerRequestResponse> getPendingRequests();

    SellerDashboardStatsDTO getSellerDashboardStats(String email);

    List<SellerOrderResponse> getSellerOrders(String email);

    MessageResponse updateOrderStatus(Long orderId, OrderStatus status, String sellerEmail);

    SellerProfileResponseDTO getSellerProfile(String email);

    MessageResponse updateSellerProfile(String email, SellerProfileUpdateDTO updateDTO);
}


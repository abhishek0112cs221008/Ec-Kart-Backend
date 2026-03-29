package com.ecommerce.ecommercebackend.seller.service;

import com.ecommerce.ecommercebackend.seller.dto.SellerRequestResponse;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SellerService {

    SellerRequestResponse requestSeller(String userEmail, String storeName,String reason , MultipartFile document) throws IOException;

    MessageResponse approveRequest(Long requestId, String adminEmail);

    MessageResponse rejectRequest(Long requestId, String adminEmail, String reason);

    List<SellerRequestResponse> getPendingRequests();
}


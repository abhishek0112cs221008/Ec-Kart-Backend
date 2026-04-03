package com.ecommerce.ecommercebackend.seller.Controller;

import com.ecommerce.ecommercebackend.seller.dto.SellerRequestResponse;
import com.ecommerce.ecommercebackend.seller.service.SellerService;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import lombok.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.ecommercebackend.seller.dto.SellerDashboardStatsDTO;
import com.ecommerce.ecommercebackend.seller.dto.SellerOrderResponse;
import com.ecommerce.ecommercebackend.Order.entity.OrderStatus;

import java.io.IOException;
import java.util.List;
import com.ecommerce.ecommercebackend.seller.dto.SellerProfileResponseDTO;
import com.ecommerce.ecommercebackend.seller.dto.SellerProfileUpdateDTO;

/**
 * Controller for seller onboarding and dashboard metrics.
 */

@SuppressWarnings("ALL")
@RestController
@RequestMapping("/api/v1/seller")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    /**
     * Dashboard stats for the authenticated seller.
     */
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerDashboardStatsDTO> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(sellerService.getSellerDashboardStats(authentication.getName()));
    }

    /**
     * List of all orders for the authenticated seller.
     */
    @GetMapping("/dashboard/orders")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<SellerOrderResponse>> getSellerOrders(Authentication authentication) {
        return ResponseEntity.ok(sellerService.getSellerOrders(authentication.getName()));
    }

    /**
     * Get the authenticated seller's profile.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerProfileResponseDTO> getSellerProfile(Authentication authentication) {
        return ResponseEntity.ok(sellerService.getSellerProfile(authentication.getName()));
    }

    /**
     * Update the authenticated seller's profile.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<MessageResponse> updateSellerProfile(
            @RequestBody SellerProfileUpdateDTO updateDTO,
            Authentication authentication
    ) {
        return ResponseEntity.ok(sellerService.updateSellerProfile(authentication.getName(), updateDTO));
    }


    /**
     * Update the status of an order (SELLER only).
     */
    @PatchMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<MessageResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(sellerService.updateOrderStatus(orderId, status, authentication.getName()));
    }


    /**
     * Submit a request to become a seller.
     *
     * The authenticated user's email is extracted from the JWT token (Authentication#getName()).
     * The request accepts multipart/form-data: a required store name and an uploaded verification
     * document; an optional reason/note can be included to explain the application.
     *
     * @param authentication Spring Security authentication object (used to get user email from the token)
     * @param storeName the requested store name the user wants to register as a seller
     * @param reason optional message or note explaining why the user wants to become a seller
     * @param document verification document (e.g., ID or KYC document) uploaded as multipart file
     * @return a {@link com.ecommerce.ecommercebackend.seller.dto.SellerRequestResponse} describing the created request
     * @throws IOException if an error occurs while uploading the provided document
     */
    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerRequestResponse> requestSeller(
            Authentication authentication,
            @RequestParam String storeName,
            @RequestParam(required = false) String reason,
            @RequestPart MultipartFile document

    ) throws IOException {

        String email = authentication.getName();

        return ResponseEntity.ok(
                sellerService.requestSeller(email, storeName,reason ,document)
        );
    }


    /**
     * List all pending seller requests (ADMIN only).
     *
     * Returns a list of pending seller requests for admin review. This endpoint is
     * secured and should only be accessible by users with ADMIN privileges.
     *
     * @return a list of {@link com.ecommerce.ecommercebackend.seller.dto.SellerRequestResponse} for requests in PENDING status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/seller-requests")
    public ResponseEntity<List<SellerRequestResponse>> listPending() {
        return ResponseEntity.ok(sellerService.getPendingRequests());
    }


    /**
     * Approve a seller request (ADMIN only).
     *
     * Approving a request promotes the associated user to {@code ROLE_SELLER}, creates or updates
     * the seller profile, marks the request as APPROVED, records the reviewing admin and timestamp,
     * and notifies the user by email.
     *
     * @param authentication Spring Security authentication (admin) — used to get the admin email for auditing
     * @param requestId the id of the seller request to approve
     * @return a {@link com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse} with success message
     * @throws com.ecommerce.ecommercebackend.exception.ResourceNotFoundException if the request does not exist
     * @throws com.ecommerce.ecommercebackend.seller.exception.SellerRequestException if the request has already been processed or is invalid
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<MessageResponse> approve(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(
                sellerService.approveRequest(requestId, authentication.getName())
        );
    }


    /**
     * Reject a seller request (ADMIN only).
     *
     * Rejects the specified seller request, records the rejection reason (if provided),
     * records the reviewing admin and timestamp, and notifies the user by email.
     *
     * @param authentication Spring Security authentication (admin) — used to get the admin email for auditing
     * @param requestId the id of the seller request to reject
     * @param reason optional human-readable reason for rejection (stored and emailed to the user)
     * @return a {@link com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse} with result message
     * @throws com.ecommerce.ecommercebackend.exception.ResourceNotFoundException if the request does not exist
     * @throws com.ecommerce.ecommercebackend.seller.exception.SellerRequestException if the request has already been processed or is invalid
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<MessageResponse> reject(
            Authentication authentication,
            @PathVariable Long requestId,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(
                sellerService.rejectRequest(requestId, authentication.getName(), reason)
        );
    }
}


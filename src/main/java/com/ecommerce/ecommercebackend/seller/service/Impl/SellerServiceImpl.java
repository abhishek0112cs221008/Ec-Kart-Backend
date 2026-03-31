package com.ecommerce.ecommercebackend.seller.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import com.ecommerce.ecommercebackend.auth.exception.UserNotFoundException;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import com.ecommerce.ecommercebackend.seller.dto.SellerRequestResponse;
import com.ecommerce.ecommercebackend.seller.entity.SellerProfile;
import com.ecommerce.ecommercebackend.seller.entity.SellerRequest;
import com.ecommerce.ecommercebackend.seller.entity.SellerRequestStatus;
import com.ecommerce.ecommercebackend.seller.exception.SellerRequestException;
import com.ecommerce.ecommercebackend.seller.repository.SellerProfileRepo;
import com.ecommerce.ecommercebackend.seller.repository.SellerRequestRepo;
import com.ecommerce.ecommercebackend.seller.service.SellerService;
import com.ecommerce.ecommercebackend.util.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SellerService implementation.
 *
 * Handles seller onboarding requests, admin approval/rejection,
 * role promotion, and seller profile creation.
 */
@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private final UsersRepo usersRepo;
    private final SellerRequestRepo sellerRequestRepo;
    private final SellerProfileRepo sellerProfileRepo;
    private final Cloudinary cloudinary;
    private final EmailService emailService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Submit a seller request by an authenticated user.
     *
     * @param userEmail email extracted from JWT token
     * @param storeName requested store name
     * @param document verification document
     * @return SellerRequestResponse
     * @throws IOException if document upload fails
     */
    @Override
    @Transactional
    public SellerRequestResponse requestSeller(String userEmail, String storeName,String reason , MultipartFile document) throws IOException {

        Users user = usersRepo.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        sellerRequestRepo.findByUser(user)
                .filter(r -> r.getStatus() == SellerRequestStatus.PENDING)
                .ifPresent(r -> {
                    throw new SellerRequestException("You already have a pending seller request");
                });

        String documentUrl = null;
        if (document != null && !document.isEmpty()) {
            Map upload = cloudinary.uploader().upload(
                    document.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "SellerRequests",
                            "public_id", userEmail + "_" + UUID.randomUUID(),
                            "overwrite", true,
                            "resource_type", "auto"
                    )
            );
            documentUrl = (String) upload.get("secure_url");
        }

        SellerRequest request = SellerRequest.builder()
                .user(user)
                .storeName(storeName)
                .reason(reason)
                .documentUrl(documentUrl)
                .status(SellerRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        sellerRequestRepo.save(request);

        return toDto(request);
    }

    /**
     * Approve a seller request (ADMIN).
     *
     * @param requestId seller request ID
     * @param adminEmail admin email from JWT
     * @return MessageResponse
     */
    @Override
    @Transactional
    public MessageResponse approveRequest(Long requestId, String adminEmail) {

        SellerRequest request = sellerRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerRequest", "id", requestId));

        if (request.getStatus() != SellerRequestStatus.PENDING) {
            throw new SellerRequestException("Seller request already processed");
        }

        Users user = request.getUser();

        // 1. Look for existing profile. Using a direct lookup to ensure we don't try to create a duplicate.
        SellerProfile profile = sellerProfileRepo.findByUser(user).orElse(null);

        if (profile == null) {
            profile = SellerProfile.builder()
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
        }


        profile.setStoreName(request.getStoreName());
        sellerProfileRepo.save(profile);

        user.setRole(Role.ROLE_SELLER);
        user.setSellerVerified(true);
        usersRepo.save(user);

        request.setStatus(SellerRequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminEmail);
        sellerRequestRepo.save(request);

        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Seller Request Approved",
                    "Congratulations! Your seller request has been approved."
            );
        } catch (Exception e) {
            System.err.println("Approval email notification failed: " + e.getMessage());
        }

        return new MessageResponse("Seller request approved successfully");
    }


    /**
     * Reject a seller request (ADMIN).
     *
     * @param requestId seller request ID
     * @param adminEmail admin email from JWT
     * @param reason optional rejection reason
     * @return MessageResponse
     */
    @Override
    @Transactional
    public MessageResponse rejectRequest(Long requestId, String adminEmail, String reason) {

        SellerRequest request = sellerRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SellerRequest", "id", requestId));

        if (request.getStatus() != SellerRequestStatus.PENDING) {
            throw new SellerRequestException("Seller request already processed");
        }

        request.setStatus(SellerRequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(adminEmail);
        request.setReason(reason);

        sellerRequestRepo.save(request);

        try {
            emailService.sendEmail(
                    request.getUser().getEmail(),
                    "Seller Request Rejected",
                    "Your seller request was rejected."
                            + (reason != null ? "\nReason: " + reason : "")
            );
        } catch (Exception e) {
            System.err.println("Rejection email notification failed: " + e.getMessage());
        }

        return new MessageResponse("Seller request rejected");
    }




    @Override
    @Transactional
    public List<SellerRequestResponse> getPendingRequests() {
        // 1. Fetch current pending requests from the database
        List<SellerRequest> pendingRequests = sellerRequestRepo.findAllByStatus(SellerRequestStatus.PENDING);

        // 2. Backfill: Identify users with ROLE_SELLER who are NOT yet verified and miss a request record
        // This handles users who registered before the seller request system was fully integrated.
        List<Users> unverifiedSellers = usersRepo.findByRoleAndSellerVerifiedFalse(Role.ROLE_SELLER);

        for (Users seller : unverifiedSellers) {
            if (sellerRequestRepo.findByUser(seller).isEmpty()) {
                SellerRequest stub = SellerRequest.builder()
                        .user(seller)
                        .storeName(seller.getFirstName() + "'s Store")
                        .status(SellerRequestStatus.PENDING)
                        .createdAt(seller.getCreatedAt() != null ? seller.getCreatedAt() : LocalDateTime.now())
                        .reason("Backfilled: Record missing for existing unverified seller.")
                        .build();
                sellerRequestRepo.save(stub);
                pendingRequests.add(stub);
            }
        }

        // Convert to DTOs
        return pendingRequests.stream()
                .map(this::toDto)
                .toList();
    }
    /* ========================= Helper ========================= */


    private SellerRequestResponse toDto(SellerRequest r) {
        return new SellerRequestResponse(
                r.getId(),
                r.getUser().getEmail(),
                r.getStoreName(),
                r.getDocumentUrl(),
                r.getReason(),
                r.getStatus(),
                r.getCreatedAt().format(ISO),
                r.getReviewedAt() != null ? r.getReviewedAt().format(ISO) : null,
                r.getReviewedBy()
        );
    }
}


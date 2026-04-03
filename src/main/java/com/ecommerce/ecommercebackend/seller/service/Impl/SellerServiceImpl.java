package com.ecommerce.ecommercebackend.seller.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.ecommercebackend.Order.entity.OrderItem;
import com.ecommerce.ecommercebackend.Order.entity.Order;
import com.ecommerce.ecommercebackend.Order.entity.OrderStatus;
import com.ecommerce.ecommercebackend.Order.repository.OrderItemRepository;
import com.ecommerce.ecommercebackend.Order.repository.OrderRepository;
import com.ecommerce.ecommercebackend.Product.repository.ProductRepository;
import com.ecommerce.ecommercebackend.auth.dto.Responses.MessageResponse;
import com.ecommerce.ecommercebackend.auth.exception.UserNotFoundException;
import com.ecommerce.ecommercebackend.entity.Role;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import com.ecommerce.ecommercebackend.seller.dto.SellerDashboardStatsDTO;
import com.ecommerce.ecommercebackend.seller.dto.SellerOrderResponse;
import com.ecommerce.ecommercebackend.seller.dto.SellerProfileResponseDTO;
import com.ecommerce.ecommercebackend.seller.dto.SellerProfileUpdateDTO;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

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
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SellerDashboardStatsDTO getSellerDashboardStats(String email) {
        BigDecimal revenue = orderItemRepository.calculateTotalRevenueBySellerEmail(email);
        long totalOrders = orderItemRepository.countOrdersBySellerEmail(email);
        long activeProducts = productRepository.countBySellerEmailAndActiveTrue(email);

        List<OrderItem> items = orderItemRepository.findAllBySellerEmail(email);
        
        // Null-safe status grouping
        Map<String, Long> statusCounts = items.stream()
                .filter(oi -> oi.getOrder() != null && oi.getOrder().getStatus() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        oi -> oi.getOrder().getStatus().name(),
                        java.util.stream.Collectors.counting()
                ));

        return SellerDashboardStatsDTO.builder()
                .totalRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .activeProducts(activeProducts)
                .ordersByStatus(statusCounts)
                .build();
    }

    @Override
    @Transactional
    public List<SellerOrderResponse> getSellerOrders(String email) {
        List<OrderItem> items = orderItemRepository.findAllBySellerEmail(email);
        return items.stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponse updateOrderStatus(Long orderId, OrderStatus status, String sellerEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Verify that this seller owns at least one item in this order
        boolean ownsItem = order.getItems().stream()
                .anyMatch(oi -> oi.getProduct().getSeller().getEmail().equalsIgnoreCase(sellerEmail));

        if (!ownsItem) {
            throw new SellerRequestException("You are not authorized to update this order");
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return new MessageResponse("Order status updated to " + status);
    }

    @Override
    public SellerProfileResponseDTO getSellerProfile(String email) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        SellerProfile profile = sellerProfileRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", "user_id", user.getId()));

        return SellerProfileResponseDTO.builder()
                .id(profile.getId())
                .storeName(profile.getStoreName())
                .bio(profile.getBio())
                .contactEmail(profile.getContactEmail())
                .contactPhone(profile.getContactPhone())
                .logoUrl(profile.getLogoUrl())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse updateSellerProfile(String email, SellerProfileUpdateDTO updateDTO) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        SellerProfile profile = sellerProfileRepo.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("SellerProfile", "user_id", user.getId()));

        profile.setStoreName(updateDTO.getStoreName());
        profile.setBio(updateDTO.getBio());
        profile.setContactEmail(updateDTO.getContactEmail());
        profile.setContactPhone(updateDTO.getContactPhone());

        sellerProfileRepo.save(profile);

        return new MessageResponse("Seller profile updated successfully");
    }


    /* ========================= Helpers ========================= */

    private SellerOrderResponse toOrderResponse(OrderItem oi) {
        BigDecimal unitPrice = oi.getPriceAtPurchase() != null ? oi.getPriceAtPurchase() : oi.getProduct().getPrice();
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;

        return SellerOrderResponse.builder()
                .orderId(oi.getOrder().getId())
                .productTitle(oi.getProduct().getName())
                .customerName(oi.getOrder().getUser().getFirstName() + " " + oi.getOrder().getUser().getLastName())
                .shippingAddress(oi.getOrder().getShippingAddress())
                .quantity(oi.getQuantity())
                .price(unitPrice.multiply(BigDecimal.valueOf(oi.getQuantity())))
                .status(oi.getOrder().getStatus())
                .createdAt(oi.getOrder().getCreatedAt())
                .build();
    }


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


package com.ecommerce.ecommercebackend.auth.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.ecommercebackend.auth.dto.Requests.*;
import com.ecommerce.ecommercebackend.auth.dto.Responses.*;
import com.ecommerce.ecommercebackend.auth.exception.*;
import com.ecommerce.ecommercebackend.entity.*;
import com.ecommerce.ecommercebackend.exception.*;
import com.ecommerce.ecommercebackend.auth.service.AuthService;
import com.ecommerce.ecommercebackend.seller.entity.SellerRequest;
import com.ecommerce.ecommercebackend.seller.entity.SellerRequestStatus;
import com.ecommerce.ecommercebackend.seller.repository.SellerRequestRepo;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import com.ecommerce.ecommercebackend.repository.VerificationTokenRepo;
import com.ecommerce.ecommercebackend.repository.OTPRepository;
import com.ecommerce.ecommercebackend.repository.PasswordResetAttemptRepository;
import com.ecommerce.ecommercebackend.util.EmailService;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Authentication and user account service implementation.
 */
@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsersRepo usersRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationTokenRepo verificationTokenRepo;
    private final AuthenticationManager authenticationManager;
    private final JWTserviceImpl jwtService;
    private final Cloudinary cloudinary;
    private final OTPRepository otpRepository;
    private final PasswordResetAttemptRepository attemptRepo;
    private final SellerRequestRepo sellerRequestRepo;

    @org.springframework.beans.factory.annotation.Value("${app.backend-url}")
    private String backendUrl;

    @Override
    @Transactional
    public RegisterResponse register(SignUpRequest signUpRequest, MultipartFile file) throws IOException {
        if (usersRepo.findByEmail(signUpRequest.getEmail()).isPresent()) {
            throw new EmailAlreadyUsedException("The new email is already in use.");
        }
        Role userRole = signUpRequest.getRole() != null ? signUpRequest.getRole() : Role.ROLE_USER;
        Users user = Users.builder()
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .email(signUpRequest.getEmail())
                .role(userRole)
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .enabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        // save user first to generate UUID
        user.setLastVerificationSentAt(LocalDateTime.now());
        usersRepo.save(user);

        // Upload image using User UUID as public_id (more robust)
        if (file != null && !file.isEmpty()) {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "Customers",
                            "public_id", user.getId().toString(),
                            "overwrite", true,
                            "resource_type", "image"));
            String imageUrl = (String) uploadResult.get("secure_url");
            user.setProfileImageUrl(imageUrl);
            usersRepo.save(user); // update with image URL
        }

        // generate and store token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationTokenRepo.save(verificationToken);

        if (userRole == Role.ROLE_SELLER) {
            SellerRequest sellerRequest = SellerRequest.builder()
                    .user(user)
                    .storeName(user.getFirstName() + "'s Store")
                    .status(SellerRequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            sellerRequestRepo.save(sellerRequest);
        }

        sendVerificationEmail(user, token);
        return new RegisterResponse("User registered. Please check your email for verification.", token);
    }

    private void sendVerificationEmail(Users user, String token) {
        String verificationLink = backendUrl + "/api/v1/auth/verify-email?token=" + token;
        String body = "Hello " + user.getFirstName() + ",\n\n" +
                "Click the link to verify your account:\n" + verificationLink +
                "\n\nIf you did not register, ignore this email.";
        try {
            emailService.sendEmail(user.getEmail(), "Verify your account", body);
        } catch (Exception e) {
            System.err.println("Email could not be sent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public MessageResponse verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (verificationToken.isUsed() || verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return new MessageResponse("Token invalid or expired");
        }
        Users user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEnabled(true);
        usersRepo.save(user);
        verificationToken.setUsed(true);
        verificationTokenRepo.save(verificationToken);
        return new MessageResponse("Email verified successfully!");
    }

    @Override
    @Transactional
    public MessageResponse resendVerificationEmail(String email) {
        Users user = usersRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified.");
        }
        if (user.getLastVerificationSentAt() != null &&
                user.getLastVerificationSentAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            throw new TooManyRequestsException("Please wait at least 1 minute before requesting another verification email.");
        }
        VerificationToken verificationToken = verificationTokenRepo.findByUser(user).orElse(new VerificationToken());
        String newToken = UUID.randomUUID().toString();
        verificationToken.setUser(user);
        verificationToken.setToken(newToken);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationToken.setUsed(false);
        verificationTokenRepo.save(verificationToken);
        user.setLastVerificationSentAt(LocalDateTime.now());
        usersRepo.save(user);
        sendVerificationEmail(user, newToken);
        return new MessageResponse("Verification email resent successfully. Please check your inbox.");
    }

    @Override
    public LoginResponse login(SignInRequest signInRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getEmail(), signInRequest.getPassword()));
        } catch (AuthenticationException ex) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        Users user = usersRepo.findByEmail(signInRequest.getEmail()).orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new AccountNotVerifiedException("Please verify your email first");
        }
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return LoginResponse.builder()
                .id(user.getId())
                .message("Login successful")
                .accessToken(token)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .sellerVerified(user.isSellerVerified())
                .build();
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenReq refreshTokenReq) {
        String email;
        try {
            email = jwtService.extractUsername(refreshTokenReq.getToken());
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        Users user = usersRepo.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        if (!jwtService.validateToken(refreshTokenReq.getToken(), user)) {
            throw new InvalidTokenException("Refresh token expired or invalid");
        }
        String newAccessToken = jwtService.generateToken(user);
        return LoginResponse.builder()
                .id(user.getId())
                .message("Token refreshed successfully")
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenReq.getToken())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .sellerVerified(user.isSellerVerified())
                .build();
    }

    @Override
    public MessageResponse logout(String email, String refreshToken) {
        return new MessageResponse("Logged out successfully");
    }

    @Override
    @Transactional
    public MessageResponse updatePassword(UpdatePasswordRequest request, String currentUserEmail) {
        Users user = usersRepo.findByEmail(currentUserEmail).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usersRepo.save(user);
        return new MessageResponse("Password updated successfully");
    }

    @Override
    @Transactional
    public UpdateProfileResponse updateProfile(String userEmail, String firstName, String lastName, String phoneNumber, MultipartFile file) throws IOException {
        Users user = usersRepo.findByEmail(userEmail).orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phoneNumber != null) user.setPhoneNumber(phoneNumber);
        if (file != null && !file.isEmpty()) {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "Customers",
                            "public_id", user.getId().toString(),
                            "overwrite", true,
                            "resource_type", "image"));
            String imageUrl = (String) uploadResult.get("secure_url");
            user.setProfileImageUrl(imageUrl);
        }
        usersRepo.save(user);
        return UpdateProfileResponse.builder().id(user.getId()).message("Profile updated successfully").firstName(user.getFirstName()).lastName(user.getLastName()).profileImageUrl(user.getProfileImageUrl()).phoneNumber(user.getPhoneNumber()).build();
    }

    @Override
    public GetProfileResponse getUserProfile(String email) {
        Users user = usersRepo.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return GetProfileResponse.builder().id(user.getId()).email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getLastName()).profileImageUrl(user.getProfileImageUrl()).role(user.getRole()).sellerVerified(user.isSellerVerified()).phoneNumber(user.getPhoneNumber()).build();
    }

    @Override
    @Transactional
    public MessageResponse deleteCurrentUser(String email) {
        Users user = usersRepo.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        verificationTokenRepo.deleteByUser(user);
        usersRepo.delete(user);
        return new MessageResponse(" User Deleted Successfully :( ");
    }

    @Override
    @Transactional
    public UpdateEmailRequest requestEmailUpdate(String currentEmail, String newEmail) {
        Users user = usersRepo.findByEmail(currentEmail).orElseThrow(() -> new UserNotFoundException("User not found"));
        if (usersRepo.findByEmail(newEmail).isPresent()) {
            throw new EmailAlreadyUsedException("The new email is already in use.");
        }
        VerificationToken token = verificationTokenRepo.findByUser(user).orElse(new VerificationToken());
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUsed(false);
        token.setNewEmail(newEmail);
        verificationTokenRepo.save(token);
        String verificationLink = backendUrl + "/api/v1/auth/update-email/verify?token=" + token;
        String body = "Hello " + user.getFirstName() + ",\n\n" +
                "Click the link to verify your account:\n" + verificationLink +
                "\n\nIf you did not try to change your email , ignore this email.";
        emailService.sendEmail(newEmail, "Verify your email", "Click to verify: " + body);
        return new UpdateEmailRequest("Verification email sent. Please check your inbox to confirm your new email.", newEmail, token.getToken());
    }

    @Override
    @Transactional
    public UpdateEmailResponse verifyEmailUpdate(String tokenStr) {
        VerificationToken token = verificationTokenRepo.findByToken(tokenStr).orElseThrow(() -> new InvalidTokenException("The verification token is invalid or expired."));
        if (token.isUsed()) throw new InvalidTokenException("This token has already been used.");
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) throw new InvalidTokenException("This token has expired.");
        Users user = token.getUser();
        user.setEmail(token.getNewEmail());
        token.setUsed(true);
        usersRepo.save(user);
        verificationTokenRepo.save(token);
        return new UpdateEmailResponse("Email updated successfully", user.getEmail());
    }

    @Override
    public @Nullable List<Users> getAllUsers() {
        return usersRepo.findAll();
    }

    @Transactional
    @Override
    public ForgetPasswordResponse forgotPassword(String currentEmail) {
        Users user = usersRepo.findByEmail(currentEmail).orElseThrow(() -> new InvalidCredentialsException("User not found with email: " + currentEmail));
        PasswordResetAttempt attempt = attemptRepo.findByEmail(currentEmail).orElse(new PasswordResetAttempt());
        if (attempt.getAttempts() >= 3 && attempt.getLastAttempt().isAfter(LocalDateTime.now().minusHours(1))) {
            throw new TooManyRequestsException("Too many reset attempts. Try again later.");
        }
        attempt.setEmail(currentEmail);
        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastAttempt(LocalDateTime.now());
        attemptRepo.save(attempt);
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        otpRepository.deleteByUser(user);
        OTP otpEntity = new OTP();
        otpEntity.setUser(user);
        otpEntity.setOtp(otp);
        otpEntity.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        otpRepository.save(otpEntity);
        String body = "Hello " + user.getFirstName() + ",\n\n" + "Your password reset code (OTP) is: " + otp + "\n\n" + "This code expires in 15 minutes.";
        emailService.sendEmail(user.getEmail(), "Password Reset OTP", body);
        return new ForgetPasswordResponse("OTP sent successfully. Please check your inbox.");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        Users user = usersRepo.findByEmail(request.getEmail()).orElseThrow(() -> new InvalidCredentialsException("User not found"));
        OTP otpEntity = otpRepository.findByUserAndOtp(user, request.getOtp()).orElseThrow(() -> new InvalidOtpException("Invalid OTP"));
        if (otpEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otpEntity);
            throw new InvalidOtpException("OTP expired");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usersRepo.save(user);
        otpRepository.delete(otpEntity);
        return new MessageResponse("Password reset successfully");
    }
}

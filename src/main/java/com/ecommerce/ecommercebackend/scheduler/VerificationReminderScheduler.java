package com.ecommerce.ecommercebackend.scheduler;

import com.ecommerce.ecommercebackend.auth.service.AuthService;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VerificationReminderScheduler {

    private final UsersRepo usersRepo;
    private final AuthService authService;

    /**
     * Runs every day at midnight to remind users who haven't verified their email.
     * Threshold: 24 hours since account creation.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void sendVerificationReminders() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        List<Users> unverifiedUsers = usersRepo.findByEmailVerifiedFalseAndCreatedAtBefore(twentyFourHoursAgo);

        for (Users user : unverifiedUsers) {
            // Only send reminder if one hasn't been sent in the last 24 hours to avoid spamming
            if (user.getLastVerificationSentAt() == null || 
                user.getLastVerificationSentAt().isBefore(LocalDateTime.now().minusHours(24))) {
                try {
                    authService.resendVerificationEmail(user.getEmail());
                } catch (Exception e) {
                    System.err.println("Failed to send scheduled reminder to: " + user.getEmail() + " Error: " + e.getMessage());
                }
            }
        }
    }
}

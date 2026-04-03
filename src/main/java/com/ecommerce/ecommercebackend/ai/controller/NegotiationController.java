package com.ecommerce.ecommercebackend.ai.controller;

import com.ecommerce.ecommercebackend.ai.dto.NegotiationRequest;
import com.ecommerce.ecommercebackend.ai.dto.NegotiationResponse;
import com.ecommerce.ecommercebackend.ai.service.NegotiationService;
import com.ecommerce.ecommercebackend.ai.entity.NegotiatedOffer;
import com.ecommerce.ecommercebackend.ai.repository.NegotiatedOfferRepository;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.repository.UsersRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/negotiation")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NegotiationController {

    private final NegotiationService negotiationService;
    private final NegotiatedOfferRepository negotiatedOfferRepository;
    private final UsersRepo usersRepository;

    @PostMapping("/chat")
    public ResponseEntity<NegotiationResponse> negotiate(@RequestBody NegotiationRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(negotiationService.negotiate(request, principal.getName()));
    }

    @GetMapping("/active-offers")
    public ResponseEntity<List<NegotiatedOffer>> getActiveOffers(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(List.of());
        }
        Users user = usersRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<NegotiatedOffer> offers = negotiatedOfferRepository.findByUserAndExpiryDateAfter(user, LocalDateTime.now());
        return ResponseEntity.ok(offers);
    }

}

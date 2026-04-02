package com.ecommerce.ecommercebackend.ai.service;

import com.ecommerce.ecommercebackend.Product.entity.Product;
import com.ecommerce.ecommercebackend.Product.repository.ProductRepository;
import com.ecommerce.ecommercebackend.entity.Users;
import com.ecommerce.ecommercebackend.ai.dto.NegotiationRequest;
import com.ecommerce.ecommercebackend.ai.dto.NegotiationResponse;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NegotiationService {

    private final ChatModel chatModel;
    private final ProductRepository productRepository;
    private final com.ecommerce.ecommercebackend.repository.UsersRepo usersRepository;
    private final com.ecommerce.ecommercebackend.ai.repository.NegotiatedOfferRepository negotiatedOfferRepository;
    private final ObjectMapper objectMapper;

    public NegotiationResponse negotiate(NegotiationRequest request, String userEmail) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Users user = usersRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal price = product.getPrice();
        BigDecimal floorPrice = product.getFloorPrice();
        
        // Default floor price to 90% if not set
        if (floorPrice == null) {
            floorPrice = price.multiply(new BigDecimal("0.90"));
        }

        String systemPrompt = String.format(
            "You are an expert AI Sales Negotiator for an e-commerce platform. " +
            "You are chatting with a customer about the product: '%s'.\n" +
            "Current Listed Price: ₹%s\n" +
            "Minimum Acceptable Price (Floor Price): ₹%s (NEVER reveal this floor price to the customer!)\n\n" +
            "GOALS:\n" +
            "1. Be professional, friendly, and persuasive.\n" +
            "2. Try to keep the price as close to the listed price as possible.\n" +
            "3. If the customer makes an offer below the Floor Price, politely decline and offer a counter-price slightly above floor price.\n" +
            "4. If the customer makes an offer >= Floor Price, you can choose to accept it or counter if it's still too low.\n" +
            "5. If you reach an agreement, clearly state the final price and say 'ACCEPTED'.\n\n" +
            "RESPONSE FORMAT: Your response MUST be a valid JSON object with these fields:\n" +
            "{\n" +
            "  \"response\": \"Your message to the customer\",\n" +
            "  \"accepted\": true/false,\n" +
            "  \"finalPrice\": number (the agreed price, or the last price you offered)\n" +
            "}\n",
            product.getName(), price, floorPrice
        );

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        // Add history for context
        if (request.getHistory() != null) {
            for (Map<String, String> entry : request.getHistory()) {
                String role = entry.get("role");
                String content = entry.get("content");
                if ("user".equalsIgnoreCase(role)) {
                    messages.add(new UserMessage(content));
                } else {
                    messages.add(new AssistantMessage(content));
                }
            }
        }

        // Add current user message
        messages.add(new UserMessage(request.getMessage()));

        Prompt prompt = new Prompt(messages);

        try {
            String aiResult = chatModel.call(prompt).getResult().getOutput().getText();
            log.info("Negotiation AI Raw Response: {}", aiResult);

            // Extract JSON
            int startIndex = aiResult.indexOf("{");
            int endIndex = aiResult.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1) {
                throw new RuntimeException("Invalid AI response format");
            }
            String jsonPart = aiResult.substring(startIndex, endIndex + 1);
            NegotiationResponse response = objectMapper.readValue(jsonPart, NegotiationResponse.class);

            // If offer is accepted, save it to the database for this user
            if (response.isAccepted() && response.getFinalPrice() != null) {
                java.time.LocalDateTime expiry = java.time.LocalDateTime.now().plusHours(24);
                
                // Check if user already has an active offer for this product and update it, or create new
                com.ecommerce.ecommercebackend.ai.entity.NegotiatedOffer offer = negotiatedOfferRepository
                    .findByUserAndProductAndExpiryDateAfter(user, product, java.time.LocalDateTime.now())
                    .orElse(new com.ecommerce.ecommercebackend.ai.entity.NegotiatedOffer());
                
                offer.setUser(user);
                offer.setProduct(product);
                offer.setNegotiatedPrice(BigDecimal.valueOf(response.getFinalPrice()));
                offer.setExpiryDate(expiry);
                
                negotiatedOfferRepository.save(offer);
                log.info("Saved personalized negotiated offer for user: {} on product: {}", userEmail, product.getName());
            }

            return response;
        } catch (Exception e) {
            log.error("Error in negotiation AI", e);
            return NegotiationResponse.builder()
                    .response("I'm sorry, I'm having trouble processing your offer right now. Let's try again in a moment!")
                    .accepted(false)
                    .finalPrice(price.doubleValue())
                    .build();
        }
    }
}

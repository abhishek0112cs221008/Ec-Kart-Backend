package com.ecommerce.ecommercebackend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationRequest {
    private UUID productId;
    private String message;
    private List<Map<String, String>> history; // [{role: "user", content: "..."}, {role: "assistant", content: "..."}]
}

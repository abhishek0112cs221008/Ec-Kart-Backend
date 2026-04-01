package com.ecommerce.ecommercebackend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewSummaryResponse {
    private String summary;
    private List<String> pros;
    private List<String> cons;
    private String sentiment; // e.g., "Positive", "Neutral", "Mixed", "Negative"
}

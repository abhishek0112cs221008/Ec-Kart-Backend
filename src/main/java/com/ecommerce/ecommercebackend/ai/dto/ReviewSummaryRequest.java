package com.ecommerce.ecommercebackend.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewSummaryRequest {
    private List<String> reviews;
}

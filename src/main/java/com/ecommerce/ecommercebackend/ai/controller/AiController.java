package com.ecommerce.ecommercebackend.ai.controller;

import com.ecommerce.ecommercebackend.ai.dto.ReviewSummaryRequest;
import com.ecommerce.ecommercebackend.ai.dto.ReviewSummaryResponse;
import com.ecommerce.ecommercebackend.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/summarize-reviews")
    public ReviewSummaryResponse summarize(@RequestBody ReviewSummaryRequest request) {
        return aiService.summarizeReviews(request.getReviews());
    }
}

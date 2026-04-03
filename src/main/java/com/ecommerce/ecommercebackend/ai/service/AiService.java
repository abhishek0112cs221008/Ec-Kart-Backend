package com.ecommerce.ecommercebackend.ai.service;

import com.ecommerce.ecommercebackend.ai.dto.ReviewSummaryResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public ReviewSummaryResponse summarizeReviews(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return ReviewSummaryResponse.builder()
                    .summary("No reviews available to summarize.")
                    .sentiment("Neutral")
                    .build();
        }

        String allReviews = String.join("\n- ", reviews);

        String promptText = "You are an expert product review analyst. Analyze the customer reviews provided below and provide a concise summary in JSON format.\n\n"
                +
                "IMPORTANT: Your response MUST be a single valid JSON object. Do not include markdown formatting, no extra text, and no headers.\n\n"
                +
                "Format your response exactly as follows:\n" +
                "{\n" +
                "  \"summary\": \"overview text\",\n" +
                "  \"pros\": [\"pro1\", \"pro2\"],\n" +
                "  \"cons\": [\"con1\", \"con2\"],\n" +
                "  \"sentiment\": \"Positive/Negative/Mixed\"\n" +
                "}\n\n" +
                "Reviews:\n- " + allReviews;

        Prompt prompt = new Prompt(promptText);

        try {
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            log.info("AI Raw Response: {}", response);

            // Extract JSON from response (find first { and last })
            int startIndex = response.indexOf("{");
            int endIndex = response.lastIndexOf("}");

            if (startIndex == -1 || endIndex == -1) {
                throw new RuntimeException("No JSON object found in AI response");
            }

            String jsonPart = response.substring(startIndex, endIndex + 1);
            return objectMapper.readValue(jsonPart, ReviewSummaryResponse.class);
        } catch (Exception e) {
            log.error("Error generating AI summary", e);
            return ReviewSummaryResponse.builder()
                    .summary(
                            "Unable to generate a specialized summary at this time. Customers have shared varied feedback.")
                    .sentiment("Neutral")
                    .build();
        }
    }
}
// confession: I'm a powerful AI assistant :)

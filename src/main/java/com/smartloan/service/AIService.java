package com.smartloan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartloan.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private final ObjectMapper objectMapper;

    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";

    public ParseLoanResponse parseLoan(ParseLoanRequest request) {
        String text = request.getText();

        // If API key is available, use Claude for parsing
        if (anthropicApiKey != null && !anthropicApiKey.isEmpty()) {
            try {
                return parseWithClaude(text);
            } catch (Exception e) {
                log.warn("Claude API call failed, falling back to regex parser: {}", e.getMessage());
            }
        }

        // Fallback to regex-based parsing
        return parseWithRegex(text);
    }

    private ParseLoanResponse parseWithClaude(String text) {
        WebClient webClient = WebClient.builder()
                .baseUrl(ANTHROPIC_API_URL)
                .defaultHeader("x-api-key", anthropicApiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", "application/json")
                .build();

        String systemPrompt = """
            You are a loan parser. Parse the user's natural language input into structured loan data.
            Extract: borrower name/email, amount, duration, interest rate, installments, frequency.

            Respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {
              "borrowerName": "string or null",
              "borrowerEmail": "string or null",
              "amount": number or null,
              "duration": "string description or null",
              "interestRate": number (default 0),
              "installments": number or null,
              "frequency": "weekly" or "monthly" or null,
              "parsed": true/false
            }

            Examples:
            - "lend Channy $20, pay back in 2 weeks, no interest" -> {"borrowerName":"Channy","amount":20,"duration":"2 weeks","interestRate":0,"installments":2,"frequency":"weekly","parsed":true}
            - "give john@email.com 100 dollars monthly for 3 months at 5%" -> {"borrowerEmail":"john@email.com","amount":100,"duration":"3 months","interestRate":5,"installments":3,"frequency":"monthly","parsed":true}
            """;

        Map<String, Object> requestBody = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 500,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", text))
        );

        String response = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("content").get(0).path("text").asText();

            // Clean up the response - remove any markdown code blocks
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            JsonNode parsed = objectMapper.readTree(content);

            return ParseLoanResponse.builder()
                    .borrowerName(parsed.has("borrowerName") && !parsed.get("borrowerName").isNull()
                            ? parsed.get("borrowerName").asText() : null)
                    .borrowerEmail(parsed.has("borrowerEmail") && !parsed.get("borrowerEmail").isNull()
                            ? parsed.get("borrowerEmail").asText() : null)
                    .amount(parsed.has("amount") && !parsed.get("amount").isNull()
                            ? parsed.get("amount").asDouble() : null)
                    .duration(parsed.has("duration") && !parsed.get("duration").isNull()
                            ? parsed.get("duration").asText() : null)
                    .interestRate(parsed.has("interestRate") ? parsed.get("interestRate").asDouble() : 0.0)
                    .installments(parsed.has("installments") && !parsed.get("installments").isNull()
                            ? parsed.get("installments").asInt() : null)
                    .frequency(parsed.has("frequency") && !parsed.get("frequency").isNull()
                            ? parsed.get("frequency").asText() : null)
                    .parsed(parsed.has("parsed") ? parsed.get("parsed").asBoolean() : true)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse Claude response", e);
            return parseWithRegex(text);
        }
    }

    private ParseLoanResponse parseWithRegex(String text) {
        String lowerText = text.toLowerCase();

        // Parse amount
        Double amount = null;
        Pattern amountPattern = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d{2})?)|" +
                "(\\d+(?:\\.\\d{2})?)\\s*(?:dollars?|usd|bucks?)");
        Matcher amountMatcher = amountPattern.matcher(text);
        if (amountMatcher.find()) {
            String match = amountMatcher.group(1) != null ? amountMatcher.group(1) : amountMatcher.group(2);
            if (match != null) {
                amount = Double.parseDouble(match.replace(",", ""));
            }
        }

        // Parse borrower name
        String borrowerName = null;
        Pattern namePattern = Pattern.compile("(?:lend|loan|give|send)\\s+(?:to\\s+)?([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)",
                Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = namePattern.matcher(text);
        if (nameMatcher.find()) {
            borrowerName = nameMatcher.group(1);
        }

        // Parse email
        String borrowerEmail = null;
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        Matcher emailMatcher = emailPattern.matcher(text);
        if (emailMatcher.find()) {
            borrowerEmail = emailMatcher.group(1);
        }

        // Parse duration
        String duration = null;
        Integer installments = null;
        String frequency = null;
        Pattern durationPattern = Pattern.compile("(?:in|within|after)?\\s*(\\d+)\\s*(weeks?|months?|days?)",
                Pattern.CASE_INSENSITIVE);
        Matcher durationMatcher = durationPattern.matcher(text);
        if (durationMatcher.find()) {
            int num = Integer.parseInt(durationMatcher.group(1));
            String unit = durationMatcher.group(2).toLowerCase();
            duration = num + " " + unit;
            installments = num;
            frequency = unit.startsWith("week") ? "weekly" : "monthly";
        }

        // Parse interest rate
        Double interestRate = 0.0;
        if (lowerText.contains("no interest") || lowerText.contains("interest-free") ||
                lowerText.contains("interest free") || lowerText.contains("0% interest")) {
            interestRate = 0.0;
        } else {
            Pattern interestPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%?\\s*(?:interest|rate)");
            Matcher interestMatcher = interestPattern.matcher(text);
            if (interestMatcher.find()) {
                interestRate = Double.parseDouble(interestMatcher.group(1));
            }
        }

        boolean parsed = amount != null && (borrowerName != null || borrowerEmail != null);

        return ParseLoanResponse.builder()
                .borrowerName(borrowerName)
                .borrowerEmail(borrowerEmail)
                .amount(amount)
                .duration(duration)
                .interestRate(interestRate)
                .installments(installments)
                .frequency(frequency)
                .parsed(parsed)
                .build();
    }
}

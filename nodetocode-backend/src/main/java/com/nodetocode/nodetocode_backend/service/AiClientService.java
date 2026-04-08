package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Service that sends prompts to Google Gemini API and returns the generated code.
 * Uses the user's personal API key if available, otherwise falls back to the
 * application-level default key from application.properties.
 */
@Service
public class AiClientService {

    private static final Logger log = LoggerFactory.getLogger(AiClientService.class);

    /** Thrown when the Gemini API responds with an error (4xx / 5xx). */
    public static class AiException extends RuntimeException {
        public AiException(String message) { super(message); }
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.default.api-key}")
    private String defaultApiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    public AiClientService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolve which API key to use: user's own key if present, else the default.
     */
    private String resolveApiKey(User user) {
        if (user != null && user.getAiApiKey() != null && !user.getAiApiKey().isBlank()) {
            return user.getAiApiKey();
        }
        return defaultApiKey;
    }

    /**
     * Call Google Gemini API with the given prompt and return the generated text.
     *
     * @param prompt the structured prompt from PromptGeneratorService
     * @param user   the current user (may have a personal API key)
     * @return the generated code text from Gemini, or null if no API key is configured
     * @throws AiException if the Gemini API returns an error response
     */
    public String generateCode(String prompt, User user) {
        String apiKey = resolveApiKey(user);

        if (apiKey == null || apiKey.isBlank() || "YOUR_DEFAULT_GEMINI_API_KEY_HERE".equals(apiKey)) {
            log.warn("No valid API key available — skipping AI generation");
            return null; // no key configured — caller shows fallback
        }

        String url = apiUrl + "?key=" + apiKey;

        // Build request body in Gemini format
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 8192
                )
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling Gemini API — prompt length: {}, using {} key",
                    prompt.length(),
                    (user != null && user.getAiApiKey() != null && !user.getAiApiKey().isBlank()) ? "user" : "default");

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String result = extractGeneratedText(response.getBody());
                if (result != null) {
                    log.info("Gemini returned {} chars of code", result.length());
                }
                return result;
            } else {
                String msg = "Gemini API returned status " + response.getStatusCode();
                log.error("{}", msg);
                throw new AiException(msg);
            }

        } catch (HttpClientErrorException | HttpServerErrorException httpEx) {
            // Extract the real error message from Gemini's response body
            String body = httpEx.getResponseBodyAsString();
            log.error("Gemini API HTTP error {} — body: {}", httpEx.getStatusCode(), body);
            String friendlyMsg = extractGeminiErrorMessage(body, httpEx.getStatusCode().toString());
            throw new AiException(friendlyMsg);

        } catch (AiException e) {
            throw e; // re-throw our own
        } catch (Exception e) {
            log.error("Gemini API call failed unexpectedly: {}", e.getMessage(), e);
            throw new AiException("Gemini call failed: " + e.getMessage());
        }
    }

    /**
     * Try to extract the human-readable error message from a Gemini error JSON body.
     */
    private String extractGeminiErrorMessage(String body, String fallback) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode msg = root.path("error").path("message");
            if (!msg.isMissingNode() && !msg.isNull()) return msg.asText();
        } catch (Exception ignored) {}
        return fallback;
    }

    /**
     * Parse the Gemini API JSON response and extract the generated text.
     */
    private String extractGeneratedText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).get("content");
                if (content != null) {
                    JsonNode parts = content.get("parts");
                    if (parts != null && parts.isArray() && !parts.isEmpty()) {
                        String text = parts.get(0).get("text").asText();
                        // Strip markdown code fences if present
                        text = stripCodeFences(text);
                        return text;
                    }
                }
            }
            log.warn("Unexpected Gemini response structure: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
            return null;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Remove markdown code fences (```java ... ```) from AI output.
     */
    private String stripCodeFences(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        // Remove opening fence like ```java or ```python or just ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        // Remove closing fence
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}

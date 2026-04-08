package com.nodetocode.nodetocode_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Calls the Flask recommendation microservice to get topic suggestions.
 */
@Slf4j
@Service
public class RecommenderService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${recommender.api.url:http://localhost:5001/recommend}")
    private String recommenderUrl;

    public RecommenderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Calls the Flask /recommend endpoint with the user's solved topics.
     *
     * @param solvedTopics list of topic tags the user has solved (e.g. ["dp", "greedy"])
     * @return a map containing recommendedTopic, confidence, and top3 predictions
     */
    public Map<String, Object> getRecommendation(List<String> solvedTopics) {
        try {
            // Build request body
            Map<String, Object> requestBody = Map.of("current_topics", solvedTopics);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody), headers
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    recommenderUrl, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("recommendedTopic", root.get("recommended_topic").asText());
                result.put("confidence", root.get("confidence").asDouble());

                List<Map<String, Object>> top3 = new ArrayList<>();
                for (JsonNode item : root.get("top_3")) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("topic", item.get("topic").asText());
                    entry.put("probability", item.get("probability").asDouble());
                    top3.add(entry);
                }
                result.put("top3", top3);

                return result;
            }

            log.warn("Recommender API returned non-2xx: {}", response.getStatusCode());
            return Map.of("error", "Recommender service unavailable");

        } catch (Exception e) {
            log.error("Failed to call recommender API: {}", e.getMessage());
            return Map.of("error", "Recommender service unavailable: " + e.getMessage());
        }
    }
}

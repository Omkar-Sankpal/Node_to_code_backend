package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.RecommenderService;
import com.nodetocode.nodetocode_backend.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arena")
@RequiredArgsConstructor
public class RecommenderController {

    private final RecommenderService recommenderService;
    private final UserService userService;

    /**
     * POST /api/arena/recommend
     *
     * Body: { "solvedTopics": ["dp", "greedy"] }
     *
     * Returns the ML-powered recommendation for the next topic to practice.
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody RecommendRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (request.getSolvedTopics() == null || request.getSolvedTopics().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "solvedTopics must be a non-empty list"));
        }

        Map<String, Object> recommendation = recommenderService.getRecommendation(request.getSolvedTopics());
        return ResponseEntity.ok(recommendation);
    }

    @Data
    public static class RecommendRequest {
        private List<String> solvedTopics;
    }
}

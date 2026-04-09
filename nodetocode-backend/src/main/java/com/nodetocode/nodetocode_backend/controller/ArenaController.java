package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.ArenaTest;
import com.nodetocode.nodetocode_backend.model.ArenaTestMode;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.ArenaTestService;
import com.nodetocode.nodetocode_backend.service.RecommenderService;
import com.nodetocode.nodetocode_backend.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arena")
@RequiredArgsConstructor
public class ArenaController {

    private final ArenaTestService arenaTestService;
    private final UserService userService;
    private final RecommenderService recommenderService;

    /* ── Start a new test ── */
    @PostMapping("/start")
    public ResponseEntity<?> startTest(@RequestBody StartRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        ArenaTestMode mode;
        try {
            mode = ArenaTestMode.valueOf(request.getMode().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid mode: " + request.getMode()));
        }

        try {
            ArenaTest test = arenaTestService.startTest(currentUser.getId(), mode);
            return ResponseEntity.ok(toMap(test));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ── Finish / submit a test ── */
    @PostMapping("/{testId}/finish")
    public ResponseEntity<?> finishTest(@PathVariable Long testId, @RequestBody FinishRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        try {
            ArenaTest test = arenaTestService.finishTest(currentUser.getId(), testId, request.getSolvedProblemIds());
            return ResponseEntity.ok(toMap(test));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /* ── Get active test ── */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveTest() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        ArenaTest test = arenaTestService.getActiveTest(currentUser.getId());
        if (test == null) return ResponseEntity.ok(Map.of("active", false));
        Map<String, Object> result = toMap(test);
        result.put("active", true);
        return ResponseEntity.ok(result);
    }

    /* ── Get test by ID ── */
    @GetMapping("/{testId}")
    public ResponseEntity<?> getTest(@PathVariable Long testId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        try {
            ArenaTest test = arenaTestService.getTest(currentUser.getId(), testId);
            return ResponseEntity.ok(toMap(test));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /* ── Get test history ── */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        List<ArenaTest> history = arenaTestService.getHistory(currentUser.getId());
        List<Map<String, Object>> result = history.stream().map(this::toMap).toList();
        return ResponseEntity.ok(result);
    }

    /* ── Get arena stats for profile ── */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        return ResponseEntity.ok(arenaTestService.getStats(currentUser.getId()));
    }

    /* ── Get AI-powered topic recommendation ── */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody RecommendRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

        List<String> solvedTopics = request != null ? request.getSolvedTopics() : null;
        if (solvedTopics == null || solvedTopics.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No solved topics provided"));
        }

        return ResponseEntity.ok(recommenderService.getRecommendation(solvedTopics));
    }

    /* ── Helper: ArenaTest → Map ── */
    private Map<String, Object> toMap(ArenaTest t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("mode", t.getMode().name());
        m.put("problemIds", t.getProblemIds());
        m.put("durationSeconds", t.getDurationSeconds());
        m.put("totalProblems", t.getTotalProblems());
        m.put("solvedCount", t.getSolvedCount());
        m.put("score", t.getScore());
        m.put("timeBonus", t.getTimeBonus());
        m.put("badge", t.getBadge());
        m.put("active", t.getActive());
        m.put("startedAt", t.getStartedAt() != null ? t.getStartedAt().toString() : null);
        m.put("finishedAt", t.getFinishedAt() != null ? t.getFinishedAt().toString() : null);
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);

        // Send absolute timestamps so clients can do timezone-safe countdown math.
        if (t.getStartedAt() != null) {
            long startedAtEpochMs = t.getStartedAt()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            m.put("startedAtEpochMs", startedAtEpochMs);

            if (t.getDurationSeconds() != null) {
                m.put("endsAtEpochMs", startedAtEpochMs + (t.getDurationSeconds() * 1000L));
            }
        }
        return m;
    }

    @Data
    public static class StartRequest {
        private String mode;
    }

    @Data
    public static class FinishRequest {
        private List<Long> solvedProblemIds;
    }

    @Data
    public static class RecommendRequest {
        private List<String> solvedTopics;
    }
}

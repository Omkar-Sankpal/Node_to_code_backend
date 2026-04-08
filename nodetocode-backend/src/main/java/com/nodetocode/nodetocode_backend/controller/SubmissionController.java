package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.Submission;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.SubmissionService;
import com.nodetocode.nodetocode_backend.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final UserService userService;

    /**
     * Save or update user's code for a problem + language.
     * POST /api/submissions
     * Body: { problemId, language, code }
     */
    @PostMapping
    public ResponseEntity<?> saveSubmission(@RequestBody SaveRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Code must not be blank"));
        }

        CodeLanguage language;
        try {
            language = CodeLanguage.valueOf(request.getLanguage().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid language: " + request.getLanguage()));
        }

        Submission saved = submissionService.saveSubmission(
                currentUser.getId(),
                request.getProblemId(),
                language,
                request.getCode()
        );

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "problemId", saved.getProblem().getId(),
                "language", saved.getLanguage().name(),
                "code", saved.getCode(),
                "updatedAt", saved.getUpdatedAt().toString()
        ));
    }

    /**
     * Get user's saved code for a specific problem + language.
     * GET /api/submissions?problemId=1&language=JAVA
     */
    @GetMapping
    public ResponseEntity<?> getSubmission(
            @RequestParam Long problemId,
            @RequestParam String language
    ) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        CodeLanguage lang;
        try {
            lang = CodeLanguage.valueOf(language.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid language: " + language));
        }

        return submissionService.getSubmission(currentUser.getId(), problemId, lang)
                .map(s -> ResponseEntity.ok(Map.of(
                        "id", s.getId(),
                        "problemId", s.getProblem().getId(),
                        "language", s.getLanguage().name(),
                        "code", s.getCode(),
                        "updatedAt", s.getUpdatedAt().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    /**
     * Get all saved submissions for a problem (all languages).
     * GET /api/submissions/problem/{problemId}
     */
    @GetMapping("/problem/{problemId}")
    public ResponseEntity<?> getSubmissionsForProblem(@PathVariable Long problemId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<Submission> submissions = submissionService.getSubmissionsForProblem(currentUser.getId(), problemId);
        List<Map<String, Object>> result = submissions.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "problemId", s.getProblem().getId(),
                "language", s.getLanguage().name(),
                "code", s.getCode(),
                "updatedAt", s.getUpdatedAt().toString()
        )).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/solve")
    public ResponseEntity<?> markSolved(@RequestBody SaveRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        CodeLanguage language;
        try {
            language = CodeLanguage.valueOf(request.getLanguage().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid language: " + request.getLanguage()));
        }

        submissionService.markSolved(
                currentUser.getId(),
                request.getProblemId(),
                language,
                request.getCode()
        );

        return ResponseEntity.ok(Map.of("message", "Problem marked as solved"));
    }

    @GetMapping("/solved")
    public ResponseEntity<?> getSolvedProblemIds() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<Long> solvedIds = submissionService.getSolvedProblemIds(currentUser.getId());
        return ResponseEntity.ok(solvedIds);
    }

    @Data
    public static class SaveRequest {
        private Long problemId;
        private String language;
        private String code;
    }
}

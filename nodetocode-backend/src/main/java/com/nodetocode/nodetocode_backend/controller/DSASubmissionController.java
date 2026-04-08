package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.DSASubmission;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.DSASubmissionService;
import com.nodetocode.nodetocode_backend.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dsa-submissions")
@RequiredArgsConstructor
public class DSASubmissionController {

    private final DSASubmissionService dsaSubmissionService;
    private final UserService userService;

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

        DSASubmission saved = dsaSubmissionService.saveSubmission(
                currentUser.getId(),
                request.getProblemId(),
                language,
                request.getCode()
        );

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "problemId", saved.getDsaProblem().getId(),
                "language", saved.getLanguage().name(),
                "code", saved.getCode(),
                "updatedAt", saved.getUpdatedAt().toString()
        ));
    }

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

        return dsaSubmissionService.getSubmission(currentUser.getId(), problemId, lang)
                .map(s -> ResponseEntity.ok(Map.of(
                        "id", s.getId(),
                        "problemId", s.getDsaProblem().getId(),
                        "language", s.getLanguage().name(),
                        "code", s.getCode(),
                        "updatedAt", s.getUpdatedAt().toString()
                )))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    @GetMapping("/problem/{problemId}")
    public ResponseEntity<?> getSubmissionsForProblem(@PathVariable Long problemId) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<DSASubmission> submissions = dsaSubmissionService.getSubmissionsForProblem(currentUser.getId(), problemId);
        List<Map<String, Object>> result = submissions.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "problemId", s.getDsaProblem().getId(),
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

        dsaSubmissionService.markSolved(
                currentUser.getId(),
                request.getProblemId(),
                language,
                request.getCode()
        );

        return ResponseEntity.ok(Map.of("message", "DSA Problem marked as solved"));
    }

    @GetMapping("/solved")
    public ResponseEntity<?> getSolvedProblemIds() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<Long> solvedIds = dsaSubmissionService.getSolvedProblemIds(currentUser.getId());
        return ResponseEntity.ok(solvedIds);
    }

    @Data
    public static class SaveRequest {
        private Long problemId;
        private String language;
        private String code;
    }
}

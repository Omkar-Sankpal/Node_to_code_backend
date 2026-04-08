package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.dto.GraphDTO;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.AiClientService;
import com.nodetocode.nodetocode_backend.service.ProblemGraphService;
import com.nodetocode.nodetocode_backend.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/problems/{problemId}/graph")
public class ProblemGraphController {

    private static final Logger log = LoggerFactory.getLogger(ProblemGraphController.class);

    private final ProblemGraphService problemGraphService;
    private final UserService userService;
    private final AiClientService aiClientService;

    public ProblemGraphController(ProblemGraphService problemGraphService, UserService userService, AiClientService aiClientService) {
        this.problemGraphService = problemGraphService;
        this.userService = userService;
        this.aiClientService = aiClientService;
    }

    @GetMapping
    public ResponseEntity<?> loadGraph(@PathVariable Long problemId) {
        try {
            log.info("GET /api/problems/{}/graph", problemId);
            User currentUser = userService.getCurrentUser();
            GraphDTO graph = problemGraphService.loadGraph(problemId, currentUser);
            log.info("Problem graph loaded for problemId={} — {} nodes, {} edges",
                    problemId,
                    graph.getNodes() != null ? graph.getNodes().size() : 0,
                    graph.getEdges() != null ? graph.getEdges().size() : 0);
            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            log.error("Failed to load problem graph for problemId={}: {}", problemId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to load graph", "message", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> saveGraph(@PathVariable Long problemId,
                                       @RequestBody GraphDTO graphDTO) {
        try {
            log.info("PUT /api/problems/{}/graph — nodes={}, edges={}",
                    problemId,
                    graphDTO.getNodes() != null ? graphDTO.getNodes().size() : 0,
                    graphDTO.getEdges() != null ? graphDTO.getEdges().size() : 0);
            User currentUser = userService.getCurrentUser();
            GraphDTO saved = problemGraphService.saveGraph(problemId, graphDTO, currentUser);
            log.info("Problem graph saved for problemId={}", problemId);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to save problem graph for problemId={}: {}", problemId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to save graph", "message", e.getMessage()));
        }
    }

    @PostMapping("/generate-code")
    public ResponseEntity<?> generateCode(@PathVariable Long problemId,
                                          @RequestBody(required = false) GraphDTO graphDTO) {
        String prompt = null;
        try {
            log.info("POST /api/problems/{}/graph/generate-code", problemId);
            User currentUser = userService.getCurrentUser();
            prompt = problemGraphService.generateCode(problemId, graphDTO, currentUser);
            log.info("Problem code generation prompt created for problemId={}, length={}", problemId, prompt.length());

            // Call AI to generate actual code
            String generatedCode = aiClientService.generateCode(prompt, currentUser);

            if (generatedCode != null) {
                log.info("AI generated code for problemId={}, length={}", problemId, generatedCode.length());
                return ResponseEntity.ok(Map.of(
                        "prompt", prompt,
                        "generatedCode", generatedCode
                ));
            } else {
                // No API key configured
                log.info("No API key configured for problemId={}", problemId);
                return ResponseEntity.ok(Map.of(
                        "prompt", prompt,
                        "generatedCode", "",
                        "aiUnavailable", true,
                        "aiError", "No API key configured. Add your Gemini API key in Profile → AI Settings."
                ));
            }

        } catch (AiClientService.AiException aiEx) {
            log.error("AI generation failed for problemId={}: {}", problemId, aiEx.getMessage());
            return ResponseEntity.ok(Map.of(
                    "prompt", prompt != null ? prompt : "",
                    "generatedCode", "",
                    "aiUnavailable", true,
                    "aiError", aiEx.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to generate code for problemId={}: {}", problemId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to generate code", "message", e.getMessage()));
        }
    }

    // Get just the prompt for external use
    @PostMapping("/get-prompt")
    public ResponseEntity<?> getPrompt(@PathVariable Long problemId,
                                       @RequestBody(required = false) GraphDTO graphDTO) {
        try {
            log.info("POST /api/problems/{}/graph/get-prompt", problemId);
            User currentUser = userService.getCurrentUser();
            String prompt = problemGraphService.generateCode(problemId, graphDTO, currentUser);
            return ResponseEntity.ok(Map.of("prompt", prompt));
        } catch (Exception e) {
            log.error("Failed to get prompt for problemId={}: {}", problemId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get prompt", "message", e.getMessage()));
        }
    }
}

package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.dto.GraphDTO;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.AiClientService;
import com.nodetocode.nodetocode_backend.service.GraphService;
import com.nodetocode.nodetocode_backend.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/graph")
public class GraphController {

    private static final Logger log = LoggerFactory.getLogger(GraphController.class);

    private final GraphService graphService;
    private final UserService userService;
    private final AiClientService aiClientService;

    public GraphController(GraphService graphService, UserService userService, AiClientService aiClientService) {
        this.graphService = graphService;
        this.userService = userService;
        this.aiClientService = aiClientService;
    }

    // Save / overwrite the node graph for a project
    @PutMapping
    public ResponseEntity<?> saveGraph(@PathVariable Long projectId,
                                              @RequestBody GraphDTO graphDTO) {
        try {
            log.info("PUT /api/projects/{}/graph — nodes={}, edges={}",
                    projectId,
                    graphDTO.getNodes() != null ? graphDTO.getNodes().size() : 0,
                    graphDTO.getEdges() != null ? graphDTO.getEdges().size() : 0);
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            log.info("Authenticated user: {} (id={})", currentUser.getEmail(), currentUser.getId());
            GraphDTO saved = graphService.saveGraph(projectId, graphDTO, currentUser);
            log.info("Graph saved successfully for project {}", projectId);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Failed to save graph for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to save graph", "message", e.getMessage()));
        }
    }

    // Load the saved node graph for a project
    @GetMapping
    public ResponseEntity<?> loadGraph(@PathVariable Long projectId) {
        try {
            log.info("GET /api/projects/{}/graph", projectId);
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            log.info("Authenticated user: {} (id={})", currentUser.getEmail(), currentUser.getId());
            GraphDTO graph = graphService.loadGraph(projectId, currentUser);
            log.info("Graph loaded for project {} — {} nodes, {} edges",
                    projectId,
                    graph.getNodes() != null ? graph.getNodes().size() : 0,
                    graph.getEdges() != null ? graph.getEdges().size() : 0);
            return ResponseEntity.ok(graph);
        } catch (Exception e) {
            log.error("Failed to load graph for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to load graph", "message", e.getMessage()));
        }
    }

    // Re-generate the LLM prompt from saved nodes/edges
    @PostMapping("/generate-prompt")
    public ResponseEntity<?> generatePrompt(@PathVariable Long projectId) {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            String prompt = graphService.generateAndSavePrompt(projectId, currentUser);
            return ResponseEntity.ok(Map.of("prompt", prompt));
        } catch (Exception e) {
            log.error("Failed to generate prompt for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to generate prompt", "message", e.getMessage()));
        }
    }

    // Generate code: saves the graph first, then generates prompt + calls AI
    @PostMapping("/generate-code")
    public ResponseEntity<?> generateCode(@PathVariable Long projectId,
                                          @RequestBody(required = false) GraphDTO graphDTO) {
        String prompt = null;
        try {
            log.info("POST /api/projects/{}/graph/generate-code", projectId);
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            // If a graph body was provided, save it first so we generate from latest state
            if (graphDTO != null && graphDTO.getNodes() != null && !graphDTO.getNodes().isEmpty()) {
                log.info("Saving graph before code generation — {} nodes, {} edges",
                        graphDTO.getNodes().size(),
                        graphDTO.getEdges() != null ? graphDTO.getEdges().size() : 0);
                graphService.saveGraph(projectId, graphDTO, currentUser);
            }

            // Generate prompt from the saved graph
            prompt = graphService.generateAndSavePrompt(projectId, currentUser);
            log.info("Code generation prompt created for project {}, length={}", projectId, prompt.length());

            // Call AI to generate actual code
            String generatedCode = aiClientService.generateCode(prompt, currentUser);

            if (generatedCode != null) {
                log.info("AI generated code for project {}, length={}", projectId, generatedCode.length());
                return ResponseEntity.ok(Map.of(
                        "prompt", prompt,
                        "generatedCode", generatedCode
                ));
            } else {
                // No API key configured — tell frontend
                log.info("No API key configured for project {}", projectId);
                return ResponseEntity.ok(Map.of(
                        "prompt", prompt,
                        "generatedCode", "",
                        "aiUnavailable", true,
                        "aiError", "No API key configured. Add your Gemini API key in Profile → AI Settings."
                ));
            }

        } catch (AiClientService.AiException aiEx) {
            log.error("AI generation failed for project {}: {}", projectId, aiEx.getMessage());
            return ResponseEntity.ok(Map.of(
                    "prompt", prompt != null ? prompt : "",
                    "generatedCode", "",
                    "aiUnavailable", true,
                    "aiError", aiEx.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to generate code for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to generate code", "message", e.getMessage()));
        }
    }

    // Get just the prompt (for users to copy and use with their own AI externally)
    @PostMapping("/get-prompt")
    public ResponseEntity<?> getPrompt(@PathVariable Long projectId,
                                       @RequestBody(required = false) GraphDTO graphDTO) {
        try {
            log.info("POST /api/projects/{}/graph/get-prompt", projectId);
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            // Save graph first if provided
            if (graphDTO != null && graphDTO.getNodes() != null && !graphDTO.getNodes().isEmpty()) {
                graphService.saveGraph(projectId, graphDTO, currentUser);
            }

            String prompt = graphService.generateAndSavePrompt(projectId, currentUser);
            return ResponseEntity.ok(Map.of("prompt", prompt));
        } catch (Exception e) {
            log.error("Failed to get prompt for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get prompt", "message", e.getMessage()));
        }
    }
}

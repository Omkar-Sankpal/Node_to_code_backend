package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.dto.DiagramRequestDTO;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.AiClientService;
import com.nodetocode.nodetocode_backend.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/diagrams")
public class DiagramController {

    private static final Logger log = LoggerFactory.getLogger(DiagramController.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("UML", "USE_CASE", "ENTITY_RELATIONSHIP");

    private final AiClientService aiClientService;
    private final UserService userService;

    public DiagramController(AiClientService aiClientService, UserService userService) {
        this.aiClientService = aiClientService;
        this.userService = userService;
    }

    @PostMapping("/system")
    public ResponseEntity<?> generateSystemDiagram(@RequestBody DiagramRequestDTO req) {
        try {
            String diagramType = req.getDiagramType() == null ? "" : req.getDiagramType().trim().toUpperCase();
            if (!ALLOWED_TYPES.contains(diagramType)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unsupported diagram type"));
            }

            String code = req.getCode() == null ? "" : req.getCode().trim();
            if (code.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code is required to generate a diagram"));
            }

            User currentUser = userService.getCurrentUser();
            String prompt = buildPrompt(req, diagramType);
            String mermaid = aiClientService.generateCode(prompt, currentUser);

            if (mermaid == null || mermaid.isBlank()) {
                return ResponseEntity.ok(Map.of(
                        "mermaid", "",
                        "aiUnavailable", true,
                        "error", "No API key configured. Add your Gemini API key in Profile -> AI Settings."
                ));
            }

                String normalized = normalizeMermaid(diagramType, mermaid, req);
            return ResponseEntity.ok(Map.of(
                    "diagramType", diagramType,
                    "mermaid", normalized
            ));

        } catch (AiClientService.AiException aiEx) {
            log.error("Diagram generation failed: {}", aiEx.getMessage());
            return ResponseEntity.ok(Map.of(
                    "mermaid", "",
                    "aiUnavailable", true,
                    "error", aiEx.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected diagram generation error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to generate diagram",
                    "message", e.getMessage()
            ));
        }
    }

    private String buildPrompt(DiagramRequestDTO req, String diagramType) {
        String contextType = req.getContextType() == null ? "UNKNOWN" : req.getContextType().trim();
        String title = req.getTitle() == null ? "Untitled" : req.getTitle().trim();
        String description = req.getDescription() == null ? "" : req.getDescription().trim();
        String language = req.getLanguage() == null ? "" : req.getLanguage().trim();
        String code = req.getCode() == null ? "" : req.getCode();

        String diagramInstruction = switch (diagramType) {
            case "UML" -> "Create a Mermaid classDiagram that represents key classes/modules, attributes, methods, and relationships.";
            case "USE_CASE" -> "Create a Mermaid flowchart TD (NOT usecaseDiagram, NOT PlantUML). " +
                    "Use safe syntax only: Actor[Actor] and UseCase((Use Case)) nodes, with arrows Actor --> UseCase.";
            case "ENTITY_RELATIONSHIP" -> "Create a Mermaid erDiagram that captures entities, core fields, and relationships.";
            default -> "Create a Mermaid diagram.";
        };

        return "You are a software architecture assistant. " +
                diagramInstruction + "\n" +
                "Return ONLY Mermaid syntax. No markdown fences. No explanations.\n" +
                "If diagram type is USE_CASE, your FIRST line MUST be exactly: flowchart TD\n" +
                "Keep names concise and accurate to code.\n\n" +
                "Context Type: " + contextType + "\n" +
                "Title: " + title + "\n" +
                "Language: " + language + "\n" +
                "Description: " + description + "\n\n" +
                "Code:\n" + code;
    }

    private String normalizeMermaid(String diagramType, String content, DiagramRequestDTO req) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        if ("USE_CASE".equals(diagramType)) {
            return normalizeUseCase(text, req);
        }

        String mustStart = switch (diagramType) {
            case "UML" -> "classDiagram";
            case "ENTITY_RELATIONSHIP" -> "erDiagram";
            default -> "";
        };

        if (!mustStart.isBlank() && !text.toLowerCase().startsWith(mustStart.toLowerCase())) {
            return mustStart + "\n" + text;
        }
        return text;
    }

    private String normalizeUseCase(String text, DiagramRequestDTO req) {
        String cleaned = text
                .replace("@startuml", "")
                .replace("@enduml", "")
                .replace("usecaseDiagram", "")
                .replace("useCaseDiagram", "")
                .replace("→", "-->")
                .replace(';', '\n')
                .trim();

        if (cleaned.toLowerCase().startsWith("flowchart")) {
            return cleaned;
        }

        // Fallback-safe use-case diagram when model returns unsupported syntax.
        LinkedHashSet<String> useCases = new LinkedHashSet<>();
        Matcher parenMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(cleaned);
        while (parenMatcher.find() && useCases.size() < 8) {
            String uc = sanitizeLabel(parenMatcher.group(1));
            if (!uc.isBlank()) useCases.add(uc);
        }

        if (useCases.isEmpty()) {
            Matcher quotedMatcher = Pattern.compile("\"([^\"]{3,60})\"").matcher(cleaned);
            while (quotedMatcher.find() && useCases.size() < 8) {
                String uc = sanitizeLabel(quotedMatcher.group(1));
                if (!uc.isBlank()) useCases.add(uc);
            }
        }

        if (useCases.isEmpty()) {
            List<String> inferred = inferUseCasesFromCode(req != null ? req.getCode() : "");
            useCases.addAll(inferred);
        }

        if (useCases.isEmpty()) {
            useCases.add("View Data");
            useCases.add("Submit Action");
        }

        String actorLabel = "User";
        if (req != null && req.getContextType() != null && req.getContextType().equalsIgnoreCase("PROJECT")) {
            actorLabel = "Developer";
        }

        StringBuilder out = new StringBuilder("flowchart TD\n");
        out.append("  A[").append(actorLabel).append("]\n");
        int i = 1;
        for (String uc : useCases) {
            out.append("  U").append(i).append("((").append(uc).append("))\n");
            out.append("  A --> U").append(i).append("\n");
            i++;
        }
        return out.toString().trim();
    }

    private List<String> inferUseCasesFromCode(String code) {
        List<String> result = new ArrayList<>();
        if (code == null || code.isBlank()) return result;
        Matcher methodMatcher = Pattern.compile("(?:public|private|protected)?\\s*(?:static\\s+)?[A-Za-z0-9_<>\\[\\]]+\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(").matcher(code);
        while (methodMatcher.find() && result.size() < 6) {
            String name = methodMatcher.group(1);
            if ("main".equals(name)) continue;
            String label = sanitizeLabel(name.replaceAll("([a-z])([A-Z])", "$1 $2"));
            if (!label.isBlank() && !result.contains(label)) {
                result.add(label);
            }
        }
        return result;
    }

    private String sanitizeLabel(String raw) {
        if (raw == null) return "";
        String normalized = raw.replaceAll("[^A-Za-z0-9 _-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40).trim();
        }
        return normalized;
    }
}

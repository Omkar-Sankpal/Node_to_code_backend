package com.nodetocode.nodetocode_backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.nodetocode.nodetocode_backend.dto.EdgeDTO;
import com.nodetocode.nodetocode_backend.dto.NodeDTO;
import com.nodetocode.nodetocode_backend.model.Edge;
import com.nodetocode.nodetocode_backend.model.Node;
import com.nodetocode.nodetocode_backend.model.Project;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Converts a project's node/edge graph into a structured LLM prompt
 * suitable for Claude Opus 4.6 (or any capable code-generation model).
 */
@Service
public class PromptGeneratorService {

    private final ObjectMapper objectMapper;

    public PromptGeneratorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generate a deterministic, structured prompt from nodes + edges.
     */
    public String generatePrompt(Project project, List<Node> nodes, List<Edge> edges) {
        return generatePrompt(project, nodes, edges, null);
    }

    /**
     * Generate a deterministic, structured prompt from nodes + edges with optional expected output.
     */
    public String generatePrompt(Project project, List<Node> nodes, List<Edge> edges, String expectedOutput) {
        StringBuilder sb = new StringBuilder();

        String lang = project.getLanguage() != null ? project.getLanguage().name() : "JAVA";

        // ── System-level instruction ──
        sb.append("You are a senior software engineer. Generate complete, production-ready, ")
          .append("compilable code based on the following visual node-graph structure.\n\n");

        // ── Project metadata ──
        sb.append("## Project Information\n");
        sb.append("- **Title:** ").append(project.getTitle()).append("\n");
        sb.append("- **Language:** ").append(lang).append("\n");
        if (project.getDescription() != null && !project.getDescription().isBlank()) {
            sb.append("- **Description:** ").append(project.getDescription()).append("\n");
        }
        sb.append("\n");

        // ── Categorise nodes ──
        List<Node> classNodes   = new ArrayList<>();
        List<Node> functionNodes = new ArrayList<>();
        List<Node> mainNodes     = new ArrayList<>();

        for (Node n : nodes) {
            switch (n.getType()) {
                case "classNode"    -> classNodes.add(n);
                case "functionNode" -> functionNodes.add(n);
                case "mainNode"     -> mainNodes.add(n);
            }
        }

        // Build lookup maps
        Map<String, Node> nodeByReactFlowId = new LinkedHashMap<>();
        for (Node n : nodes) {
            nodeByReactFlowId.put(n.getReactFlowId(), n);
        }

        // ── Classes ──
        if (!classNodes.isEmpty()) {
            sb.append("## Classes\n");
            for (Node cls : classNodes) {
                JsonNode data = parseMetadata(cls.getMetadata());
                String name = getJsonText(data, "name", "UnnamedClass");
                String parentClass = getJsonText(data, "parentClass", null);

                sb.append("### Class: `").append(name).append("`");
                if (parentClass != null && !parentClass.isBlank()) {
                    sb.append(" extends `").append(parentClass).append("`");
                }
                sb.append("\n");
                sb.append("- Node ID: ").append(cls.getReactFlowId()).append("\n");

                // Variables
                List<String> variables = getJsonArray(data, "variables");
                if (!variables.isEmpty()) {
                    sb.append("- **Variables:**\n");
                    for (String v : variables) {
                        sb.append("  - `").append(v).append("`\n");
                    }
                }

                // Custom prompt
                if (cls.getPrompt() != null && !cls.getPrompt().isBlank()) {
                    sb.append("- **Additional Instructions:** ").append(cls.getPrompt()).append("\n");
                }
                sb.append("\n");
            }
        }

        // ── Functions / Methods ──
        if (!functionNodes.isEmpty()) {
            sb.append("## Functions / Methods\n");
            for (Node fn : functionNodes) {
                JsonNode data = parseMetadata(fn.getMetadata());
                String name       = getJsonText(data, "name", "unnamedMethod");
                String params     = getJsonText(data, "params", "");
                String returnType = getJsonText(data, "returnType", "void");
                String code       = getJsonText(data, "code", "");
                String parentClass = getJsonText(data, "parentClass", null);

                sb.append("### Function: `").append(returnType).append(" ").append(name)
                  .append("(").append(params).append(")`\n");
                sb.append("- Node ID: ").append(fn.getReactFlowId()).append("\n");

                if (parentClass != null && !parentClass.isBlank()) {
                    sb.append("- **Belongs to class:** `").append(parentClass).append("`\n");
                }
                if (code != null && !code.isBlank()) {
                    sb.append("- **Implementation hint:**\n```\n").append(code).append("\n```\n");
                }
                if (fn.getPrompt() != null && !fn.getPrompt().isBlank()) {
                    sb.append("- **Additional Instructions:** ").append(fn.getPrompt()).append("\n");
                }
                sb.append("\n");
            }
        }

        // ── Entry Points ──
        if (!mainNodes.isEmpty()) {
            sb.append("## Entry Points\n");
            for (Node m : mainNodes) {
                JsonNode data = parseMetadata(m.getMetadata());
                String code = getJsonText(data, "code", "");

                sb.append("### Main Entry Point\n");
                sb.append("- Node ID: ").append(m.getReactFlowId()).append("\n");
                if (code != null && !code.isBlank()) {
                    sb.append("- **Implementation hint:**\n```\n").append(code).append("\n```\n");
                }
                if (m.getPrompt() != null && !m.getPrompt().isBlank()) {
                    sb.append("- **Additional Instructions:** ").append(m.getPrompt()).append("\n");
                }
                sb.append("\n");
            }
        }

        // ── Relationships (Edges) ──
        if (!edges.isEmpty()) {
            sb.append("## Relationships (Edges)\n");
            for (Edge e : edges) {
                String sourceName = getNodeDisplayName(e.getSourceNode());
                String targetName = getNodeDisplayName(e.getTargetNode());

                JsonNode edgeMeta = parseMetadata(e.getMetadata());
                boolean isInheritance = false;

                // Check nested data.type for inheritance
                if (edgeMeta != null && edgeMeta.has("data")) {
                    JsonNode dataNode = edgeMeta.get("data");
                    String dataType = getJsonText(dataNode, "type", "");
                    if ("inheritance".equals(dataType)) {
                        isInheritance = true;
                    }
                }

                if (isInheritance) {
                    sb.append("- `").append(sourceName).append("` **extends** `")
                      .append(targetName).append("` (inheritance)\n");
                } else {
                    sb.append("- `").append(sourceName).append("` → `")
                      .append(targetName).append("` (connection)\n");
                }
            }
            sb.append("\n");
        }

        // ── Requirements ──
        sb.append("## Requirements\n");
        sb.append("1. Generate complete, compilable ").append(lang).append(" code.\n");
        sb.append("2. Implement ALL classes with their declared variables and proper constructors/getters/setters.\n");
        sb.append("3. Implement ALL functions/methods with the exact signatures specified.\n");
        sb.append("4. If implementation hints (body code) are provided, incorporate them into the methods.\n");
        sb.append("5. Maintain ALL inheritance relationships as specified in the edges.\n");
        sb.append("6. Include the main entry point that properly initialises and uses the classes.\n");
        sb.append("7. Follow ").append(lang).append(" best practices, clean code principles, and proper formatting.\n");
        sb.append("8. Add brief inline comments explaining complex logic.\n\n");

        sb.append("## Output Format\n");
        sb.append("Provide the complete code as a single ").append(lang.toLowerCase())
          .append(" file, ready to compile and run.\n");
        sb.append("Do NOT include explanations outside the code. Only return the raw code.\n");
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            sb.append("\n## Expected Output\n");
            sb.append("When run, the program should produce output matching:\n```\n")
              .append(expectedOutput).append("\n```\n");
        }

        return sb.toString();
    }

    /**
     * DTO-based variant — used by ProblemGraphService (no Project entity needed).
     */
    public String generatePromptFromDTOs(String title, String description, String language,
                                         List<NodeDTO> nodes, List<EdgeDTO> edges) {
        return generatePromptFromDTOs(title, description, language, nodes, edges, null);
    }

    /**
     * DTO-based variant with optional expected output format.
     */
    public String generatePromptFromDTOs(String title, String description, String language,
                                         List<NodeDTO> nodes, List<EdgeDTO> edges,
                                         String expectedOutput) {
        StringBuilder sb = new StringBuilder();
        String lang = (language != null && !language.isBlank()) ? language.toUpperCase() : "JAVA";

        sb.append("You are a senior software engineer. Generate complete, production-ready, ")
          .append("compilable code based on the following visual node-graph structure.\n\n");

        sb.append("## Problem Context\n");
        sb.append("- **Title:** ").append(title).append("\n");
        sb.append("- **Language:** ").append(lang).append("\n");
        if (description != null && !description.isBlank()) {
            sb.append("- **Description:** ").append(description).append("\n");
        }
        sb.append("\n");

        List<NodeDTO> classNodes    = new ArrayList<>();
        List<NodeDTO> functionNodes = new ArrayList<>();
        List<NodeDTO> mainNodes     = new ArrayList<>();

        for (NodeDTO n : nodes) {
            if (n.getType() == null) continue;
            switch (n.getType()) {
                case "classNode"    -> classNodes.add(n);
                case "functionNode" -> functionNodes.add(n);
                case "mainNode"     -> mainNodes.add(n);
            }
        }

        Map<String, NodeDTO> nodeByRfId = new LinkedHashMap<>();
        for (NodeDTO n : nodes) nodeByRfId.put(n.getReactFlowId(), n);

        if (!classNodes.isEmpty()) {
            sb.append("## Classes\n");
            for (NodeDTO cls : classNodes) {
                JsonNode data = parseMetadata(cls.getMetadata());
                String name = getJsonText(data, "name", "UnnamedClass");
                String parent = getJsonText(data, "parentClass", null);
                sb.append("### Class: `").append(name).append("`");
                if (parent != null && !parent.isBlank()) sb.append(" extends `").append(parent).append("`");
                sb.append("\n- Node ID: ").append(cls.getReactFlowId()).append("\n");
                List<String> vars = getJsonArray(data, "variables");
                if (!vars.isEmpty()) {
                    sb.append("- **Variables:**\n");
                    for (String v : vars) sb.append("  - `").append(v).append("`\n");
                }
                if (cls.getPrompt() != null && !cls.getPrompt().isBlank())
                    sb.append("- **Additional Instructions:** ").append(cls.getPrompt()).append("\n");
                sb.append("\n");
            }
        }

        if (!functionNodes.isEmpty()) {
            sb.append("## Functions / Methods\n");
            for (NodeDTO fn : functionNodes) {
                JsonNode data = parseMetadata(fn.getMetadata());
                String name       = getJsonText(data, "name", "unnamedMethod");
                String params     = getJsonText(data, "params", "");
                String returnType = getJsonText(data, "returnType", "void");
                String code       = getJsonText(data, "code", "");
                String parent     = getJsonText(data, "parentClass", null);
                sb.append("### Function: `").append(returnType).append(" ").append(name)
                  .append("(").append(params).append(")`\n");
                sb.append("- Node ID: ").append(fn.getReactFlowId()).append("\n");
                if (parent != null && !parent.isBlank())
                    sb.append("- **Belongs to class:** `").append(parent).append("`\n");
                if (code != null && !code.isBlank())
                    sb.append("- **Implementation hint:**\n```\n").append(code).append("\n```\n");
                if (fn.getPrompt() != null && !fn.getPrompt().isBlank())
                    sb.append("- **Additional Instructions:** ").append(fn.getPrompt()).append("\n");
                sb.append("\n");
            }
        }

        if (!mainNodes.isEmpty()) {
            sb.append("## Entry Points\n");
            for (NodeDTO m : mainNodes) {
                JsonNode data = parseMetadata(m.getMetadata());
                String code = getJsonText(data, "code", "");
                sb.append("### Main Entry Point\n- Node ID: ").append(m.getReactFlowId()).append("\n");
                if (code != null && !code.isBlank())
                    sb.append("- **Implementation hint:**\n```\n").append(code).append("\n```\n");
                if (m.getPrompt() != null && !m.getPrompt().isBlank())
                    sb.append("- **Additional Instructions:** ").append(m.getPrompt()).append("\n");
                sb.append("\n");
            }
        }

        if (!edges.isEmpty()) {
            sb.append("## Relationships (Edges)\n");
            for (EdgeDTO e : edges) {
                NodeDTO src = nodeByRfId.get(e.getSourceReactFlowId());
                NodeDTO tgt = nodeByRfId.get(e.getTargetReactFlowId());
                String srcName = getDtoDisplayName(src, e.getSourceReactFlowId());
                String tgtName = getDtoDisplayName(tgt, e.getTargetReactFlowId());
                JsonNode edgeMeta = parseMetadata(e.getMetadata());
                boolean isInheritance = false;
                if (edgeMeta != null && edgeMeta.has("data")) {
                    String dt = getJsonText(edgeMeta.get("data"), "type", "");
                    if ("inheritance".equals(dt)) isInheritance = true;
                }
                if (isInheritance)
                    sb.append("- `").append(srcName).append("` **extends** `").append(tgtName).append("` (inheritance)\n");
                else
                    sb.append("- `").append(srcName).append("` → `").append(tgtName).append("` (connection)\n");
            }
            sb.append("\n");
        }

        sb.append("## Requirements\n");
        sb.append("1. Generate complete, compilable ").append(lang).append(" code.\n");
        sb.append("2. Implement ALL classes with their declared variables and proper constructors/getters/setters.\n");
        sb.append("3. Implement ALL functions/methods with the exact signatures specified.\n");
        sb.append("4. If implementation hints are provided, incorporate them into the methods.\n");
        sb.append("5. Maintain ALL inheritance relationships as specified in the edges.\n");
        sb.append("6. Include the main entry point that properly initialises and uses the classes.\n");
        sb.append("7. Follow ").append(lang).append(" best practices and clean code principles.\n");
        sb.append("8. Add brief inline comments explaining complex logic.\n\n");
        sb.append("## Output Format\n");
        sb.append("Provide the complete code as a single ").append(lang.toLowerCase())
          .append(" file, ready to compile and run.\n");
        sb.append("Do NOT include explanations outside the code. Only return the raw code.\n");
        if (expectedOutput != null && !expectedOutput.isBlank()) {
            sb.append("\n## Expected Output\n");
            sb.append("When run, the program should produce output matching:\n```\n")
              .append(expectedOutput).append("\n```\n");
        }

        return sb.toString();
    }

    private String getDtoDisplayName(NodeDTO node, String fallbackId) {
        if (node == null) return "Node-" + fallbackId;
        JsonNode data = parseMetadata(node.getMetadata());
        String name = getJsonText(data, "name", null);
        if (name != null && !name.isBlank()) return name;
        if ("mainNode".equals(node.getType())) return "Main Entry";
        return "Node-" + node.getReactFlowId();
    }

    // ── Helpers ──

    private String getNodeDisplayName(Node node) {
        JsonNode data = parseMetadata(node.getMetadata());
        String name = getJsonText(data, "name", null);
        if (name != null && !name.isBlank()) return name;
        if ("mainNode".equals(node.getType())) return "Main Entry";
        return "Node-" + node.getReactFlowId();
    }

    private JsonNode parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            return objectMapper.readTree(metadata);
        } catch (Exception e) {
            return null;
        }
    }

    private String getJsonText(JsonNode node, String field, String defaultValue) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return defaultValue;
        return node.get(field).asText(defaultValue);
    }

    private List<String> getJsonArray(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        if (node == null || !node.has(field) || !node.get(field).isArray()) return result;
        for (JsonNode item : node.get(field)) {
            result.add(item.asText());
        }
        return result;
    }
}

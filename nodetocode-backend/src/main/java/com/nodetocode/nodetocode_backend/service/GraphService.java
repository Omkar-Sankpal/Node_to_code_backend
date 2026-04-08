package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.EdgeDao;
import com.nodetocode.nodetocode_backend.dao.NodeDao;
import com.nodetocode.nodetocode_backend.dao.ProjectDao;
import com.nodetocode.nodetocode_backend.dto.EdgeDTO;
import com.nodetocode.nodetocode_backend.dto.GraphDTO;
import com.nodetocode.nodetocode_backend.dto.NodeDTO;
import com.nodetocode.nodetocode_backend.model.Edge;
import com.nodetocode.nodetocode_backend.model.Node;
import com.nodetocode.nodetocode_backend.model.Project;
import com.nodetocode.nodetocode_backend.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    private final NodeDao nodeDao;
    private final EdgeDao edgeDao;
    private final ProjectDao projectDao;
    private final PromptGeneratorService promptGeneratorService;

    public GraphService(NodeDao nodeDao,
                        EdgeDao edgeDao,
                        ProjectDao projectDao,
                        PromptGeneratorService promptGeneratorService) {
        this.nodeDao = nodeDao;
        this.edgeDao = edgeDao;
        this.projectDao = projectDao;
        this.promptGeneratorService = promptGeneratorService;
    }

    /**
     * Save (overwrite) the full node graph for a project.
     * Deletes all existing nodes/edges, inserts the new ones.
     * Prompt/code generation is NOT done here — use generateAndSavePrompt() separately.
     */
    @Transactional
    public GraphDTO saveGraph(Long projectId, GraphDTO graphDTO, User user) {
        log.info("saveGraph called for projectId={}, nodes={}, edges={}",
                projectId,
                graphDTO.getNodes() != null ? graphDTO.getNodes().size() : 0,
                graphDTO.getEdges() != null ? graphDTO.getEdges().size() : 0);

        Project project = projectDao.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new RuntimeException("Project not found or unauthorized"));

        // 1. Delete existing edges first (FK to nodes), then nodes
        List<Edge> existingEdges = edgeDao.findByProject(project);
        if (!existingEdges.isEmpty()) {
            log.info("Deleting {} existing edges", existingEdges.size());
            edgeDao.deleteAll(existingEdges);
            edgeDao.flush();
        }

        List<Node> existingNodes = nodeDao.findByProject(project);
        if (!existingNodes.isEmpty()) {
            log.info("Deleting {} existing nodes", existingNodes.size());
            nodeDao.deleteAll(existingNodes);
            nodeDao.flush();
        }

        // 2. Save new nodes
        Map<String, Node> nodeMap = new LinkedHashMap<>();
        if (graphDTO.getNodes() != null) {
            for (NodeDTO dto : graphDTO.getNodes()) {
                Node node = new Node();
                node.setReactFlowId(dto.getReactFlowId());
                node.setType(dto.getType());
                node.setPositionX(dto.getPositionX());
                node.setPositionY(dto.getPositionY());
                node.setPrompt(dto.getPrompt());
                node.setMetadata(dto.getMetadata());
                node.setProject(project);
                node = nodeDao.save(node);
                nodeMap.put(dto.getReactFlowId(), node);
            }
            nodeDao.flush();
            log.info("Saved {} new nodes", nodeMap.size());
        }

        // 3. Save new edges (lookup source/target by reactFlowId)
        List<Edge> savedEdges = new ArrayList<>();
        if (graphDTO.getEdges() != null) {
            for (EdgeDTO dto : graphDTO.getEdges()) {
                Node source = nodeMap.get(dto.getSourceReactFlowId());
                Node target = nodeMap.get(dto.getTargetReactFlowId());
                if (source == null || target == null) {
                    log.warn("Skipping edge {} — missing source={} or target={}",
                            dto.getReactFlowId(), dto.getSourceReactFlowId(), dto.getTargetReactFlowId());
                    continue;
                }

                Edge edge = new Edge();
                edge.setReactFlowId(dto.getReactFlowId());
                edge.setSourceNode(source);
                edge.setTargetNode(target);
                edge.setMetadata(dto.getMetadata());
                edge.setProject(project);
                savedEdges.add(edgeDao.save(edge));
            }
            edgeDao.flush();
            log.info("Saved {} new edges", savedEdges.size());
        }

        // 4. Save viewport state
        project.setViewportX(graphDTO.getViewportX());
        project.setViewportY(graphDTO.getViewportY());
        project.setViewportZoom(graphDTO.getViewportZoom());
        projectDao.save(project);

        log.info("saveGraph completed successfully for projectId={}", projectId);

        // 5. Build response
        GraphDTO response = new GraphDTO();
        response.setNodes(graphDTO.getNodes());
        response.setEdges(graphDTO.getEdges());
        response.setViewportX(graphDTO.getViewportX());
        response.setViewportY(graphDTO.getViewportY());
        response.setViewportZoom(graphDTO.getViewportZoom());
        response.setGeneratedPrompt(project.getGeneratedPrompt());
        return response;
    }

    /**
     * Load the saved node graph for a project.
     */
    @Transactional(readOnly = true)
    public GraphDTO loadGraph(Long projectId, User user) {
        Project project = projectDao.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new RuntimeException("Project not found or unauthorized"));

        List<Node> nodes = nodeDao.findByProject(project);
        List<Edge> edges = edgeDao.findByProject(project);

        List<NodeDTO> nodeDTOs = nodes.stream().map(n -> {
            NodeDTO dto = new NodeDTO();
            dto.setReactFlowId(n.getReactFlowId());
            dto.setType(n.getType());
            dto.setPositionX(n.getPositionX());
            dto.setPositionY(n.getPositionY());
            dto.setPrompt(n.getPrompt());
            dto.setMetadata(n.getMetadata());
            return dto;
        }).collect(Collectors.toList());

        List<EdgeDTO> edgeDTOs = edges.stream().map(e -> {
            EdgeDTO dto = new EdgeDTO();
            dto.setReactFlowId(e.getReactFlowId());
            dto.setSourceReactFlowId(e.getSourceNode().getReactFlowId());
            dto.setTargetReactFlowId(e.getTargetNode().getReactFlowId());
            dto.setMetadata(e.getMetadata());
            return dto;
        }).collect(Collectors.toList());

        GraphDTO graphDTO = new GraphDTO();
        graphDTO.setNodes(nodeDTOs);
        graphDTO.setEdges(edgeDTOs);
        graphDTO.setGeneratedPrompt(project.getGeneratedPrompt());
        graphDTO.setViewportX(project.getViewportX());
        graphDTO.setViewportY(project.getViewportY());
        graphDTO.setViewportZoom(project.getViewportZoom());
        return graphDTO;
    }

    /**
     * Re-generate the LLM prompt from the already-saved graph.
     */
    @Transactional
    public String generateAndSavePrompt(Long projectId, User user) {
        Project project = projectDao.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new RuntimeException("Project not found or unauthorized"));

        List<Node> nodes = nodeDao.findByProject(project);
        List<Edge> edges = edgeDao.findByProject(project);

        String prompt = promptGeneratorService.generatePrompt(project, nodes, edges);
        project.setGeneratedPrompt(prompt);
        projectDao.save(project);
        return prompt;
    }
}

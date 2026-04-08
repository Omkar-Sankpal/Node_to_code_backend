package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.ProblemDao;
import com.nodetocode.nodetocode_backend.dao.ProblemEdgeDao;
import com.nodetocode.nodetocode_backend.dao.ProblemGraphDao;
import com.nodetocode.nodetocode_backend.dao.ProblemNodeDao;
import com.nodetocode.nodetocode_backend.dto.EdgeDTO;
import com.nodetocode.nodetocode_backend.dto.GraphDTO;
import com.nodetocode.nodetocode_backend.dto.NodeDTO;
import com.nodetocode.nodetocode_backend.model.Problem;
import com.nodetocode.nodetocode_backend.model.ProblemEdge;
import com.nodetocode.nodetocode_backend.model.ProblemGraph;
import com.nodetocode.nodetocode_backend.model.ProblemNode;
import com.nodetocode.nodetocode_backend.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProblemGraphService {

    private static final Logger log = LoggerFactory.getLogger(ProblemGraphService.class);

    private final ProblemGraphDao problemGraphDao;
    private final ProblemNodeDao problemNodeDao;
    private final ProblemEdgeDao problemEdgeDao;
    private final ProblemDao problemDao;
    private final PromptGeneratorService promptGeneratorService;

    public ProblemGraphService(ProblemGraphDao problemGraphDao,
                               ProblemNodeDao problemNodeDao,
                               ProblemEdgeDao problemEdgeDao,
                               ProblemDao problemDao,
                               PromptGeneratorService promptGeneratorService) {
        this.problemGraphDao = problemGraphDao;
        this.problemNodeDao = problemNodeDao;
        this.problemEdgeDao = problemEdgeDao;
        this.problemDao = problemDao;
        this.promptGeneratorService = promptGeneratorService;
    }

    @Transactional
    public GraphDTO saveGraph(Long problemId, GraphDTO graphDTO, User user) {
        log.info("saveGraph (problem) called for problemId={}, nodes={}, edges={}",
                problemId,
                graphDTO.getNodes() != null ? graphDTO.getNodes().size() : 0,
                graphDTO.getEdges() != null ? graphDTO.getEdges().size() : 0);

        Problem problem = problemDao.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));

        // Find or create the ProblemGraph record for this user+problem
        ProblemGraph pg = problemGraphDao.findByUserAndProblem(user, problem)
                .orElseGet(() -> {
                    ProblemGraph newPg = new ProblemGraph();
                    newPg.setUser(user);
                    newPg.setProblem(problem);
                    return problemGraphDao.save(newPg);
                });

        // Delete existing edges first (FK), then nodes
        List<ProblemEdge> existingEdges = problemEdgeDao.findByProblemGraph(pg);
        if (!existingEdges.isEmpty()) {
            problemEdgeDao.deleteAll(existingEdges);
            problemEdgeDao.flush();
        }
        List<ProblemNode> existingNodes = problemNodeDao.findByProblemGraph(pg);
        if (!existingNodes.isEmpty()) {
            problemNodeDao.deleteAll(existingNodes);
            problemNodeDao.flush();
        }

        // Save new nodes
        Map<String, ProblemNode> nodeMap = new LinkedHashMap<>();
        if (graphDTO.getNodes() != null) {
            for (NodeDTO dto : graphDTO.getNodes()) {
                ProblemNode node = new ProblemNode();
                node.setReactFlowId(dto.getReactFlowId());
                node.setType(dto.getType());
                node.setPositionX(dto.getPositionX());
                node.setPositionY(dto.getPositionY());
                node.setPrompt(dto.getPrompt());
                node.setMetadata(dto.getMetadata());
                node.setProblemGraph(pg);
                node = problemNodeDao.save(node);
                nodeMap.put(dto.getReactFlowId(), node);
            }
            problemNodeDao.flush();
            log.info("Saved {} problem nodes", nodeMap.size());
        }

        // Save new edges
        if (graphDTO.getEdges() != null) {
            for (EdgeDTO dto : graphDTO.getEdges()) {
                ProblemNode source = nodeMap.get(dto.getSourceReactFlowId());
                ProblemNode target = nodeMap.get(dto.getTargetReactFlowId());
                if (source == null || target == null) {
                    log.warn("Skipping edge {} — missing source/target", dto.getReactFlowId());
                    continue;
                }
                ProblemEdge edge = new ProblemEdge();
                edge.setReactFlowId(dto.getReactFlowId());
                edge.setSourceNode(source);
                edge.setTargetNode(target);
                edge.setMetadata(dto.getMetadata());
                edge.setProblemGraph(pg);
                problemEdgeDao.save(edge);
            }
            problemEdgeDao.flush();
        }

        // Save viewport
        pg.setViewportX(graphDTO.getViewportX());
        pg.setViewportY(graphDTO.getViewportY());
        pg.setViewportZoom(graphDTO.getViewportZoom());
        problemGraphDao.save(pg);

        log.info("saveGraph (problem) completed for problemId={}", problemId);

        GraphDTO response = new GraphDTO();
        response.setNodes(graphDTO.getNodes());
        response.setEdges(graphDTO.getEdges());
        response.setViewportX(graphDTO.getViewportX());
        response.setViewportY(graphDTO.getViewportY());
        response.setViewportZoom(graphDTO.getViewportZoom());
        return response;
    }

    @Transactional
    public String generateCode(Long problemId, GraphDTO graphDTO, User user) {
        log.info("generateCode (problem) called for problemId={}", problemId);

        Problem problem = problemDao.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));

        // Save graph first if nodes are provided
        if (graphDTO != null && graphDTO.getNodes() != null && !graphDTO.getNodes().isEmpty()) {
            saveGraph(problemId, graphDTO, user);
        }

        // Reload saved nodes/edges
        ProblemGraph pg = problemGraphDao.findByUserAndProblem(user, problem)
                .orElseThrow(() -> new RuntimeException("No saved graph for this problem"));

        List<ProblemNode> savedNodes = problemNodeDao.findByProblemGraph(pg);
        List<ProblemEdge> savedEdges = problemEdgeDao.findByProblemGraph(pg);

        List<NodeDTO> nodeDTOs = savedNodes.stream().map(n -> {
            NodeDTO dto = new NodeDTO();
            dto.setReactFlowId(n.getReactFlowId());
            dto.setType(n.getType());
            dto.setPositionX(n.getPositionX());
            dto.setPositionY(n.getPositionY());
            dto.setPrompt(n.getPrompt());
            dto.setMetadata(n.getMetadata());
            return dto;
        }).collect(Collectors.toList());

        List<EdgeDTO> edgeDTOs = savedEdges.stream().map(e -> {
            EdgeDTO dto = new EdgeDTO();
            dto.setReactFlowId(e.getReactFlowId());
            dto.setSourceReactFlowId(e.getSourceNode().getReactFlowId());
            dto.setTargetReactFlowId(e.getTargetNode().getReactFlowId());
            dto.setMetadata(e.getMetadata());
            return dto;
        }).collect(Collectors.toList());

        // Build a rich description from problem metadata
        StringBuilder desc = new StringBuilder(problem.getDescription());
        if (problem.getSampleInput() != null && !problem.getSampleInput().isBlank())
            desc.append("\n\nSample Input:\n").append(problem.getSampleInput());
        if (problem.getSampleOutput() != null && !problem.getSampleOutput().isBlank())
            desc.append("\n\nSample Output:\n").append(problem.getSampleOutput());

        // Pass expected output format to prompt generator
        String expectedOutput = problem.getSampleOutput();

        return promptGeneratorService.generatePromptFromDTOs(
                problem.getTitle(), desc.toString(), "JAVA", nodeDTOs, edgeDTOs, expectedOutput);
    }

    @Transactional(readOnly = true)
    public GraphDTO loadGraph(Long problemId, User user) {
        Problem problem = problemDao.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found: " + problemId));

        Optional<ProblemGraph> pgOpt = problemGraphDao.findByUserAndProblem(user, problem);

        GraphDTO graphDTO = new GraphDTO();

        if (pgOpt.isEmpty()) {
            // No saved graph yet — return empty
            graphDTO.setNodes(Collections.emptyList());
            graphDTO.setEdges(Collections.emptyList());
            return graphDTO;
        }

        ProblemGraph pg = pgOpt.get();
        List<ProblemNode> nodes = problemNodeDao.findByProblemGraph(pg);
        List<ProblemEdge> edges = problemEdgeDao.findByProblemGraph(pg);

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

        graphDTO.setNodes(nodeDTOs);
        graphDTO.setEdges(edgeDTOs);
        graphDTO.setViewportX(pg.getViewportX());
        graphDTO.setViewportY(pg.getViewportY());
        graphDTO.setViewportZoom(pg.getViewportZoom());
        return graphDTO;
    }
}

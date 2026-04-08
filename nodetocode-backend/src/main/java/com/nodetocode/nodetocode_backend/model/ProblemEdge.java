package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "problem_edges",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reactFlowId", "problem_graph_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reactFlowId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    private ProblemNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private ProblemNode targetNode;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_graph_id", nullable = false)
    private ProblemGraph problemGraph;
}

package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "problem_nodes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reactFlowId", "problem_graph_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reactFlowId;

    @Column(nullable = false)
    private String type;

    private double positionX;
    private double positionY;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_graph_id", nullable = false)
    private ProblemGraph problemGraph;
}

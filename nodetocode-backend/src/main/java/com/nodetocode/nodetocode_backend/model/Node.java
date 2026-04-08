package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(
        name = "nodes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"reactFlowId", "project_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // React Flow node ID (frontend ID)
    @Column(nullable = false)
    private String reactFlowId;

    // Node type (CLASS, METHOD, VARIABLE, etc.)
    @Column(nullable = false)
    private String type;

    // Position in canvas
    private double positionX;
    private double positionY;

    // Custom prompt for this node (important for LLM)
    @Column(columnDefinition = "TEXT")
    private String prompt;

    // Additional JSON data from React Flow
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Relationship to project
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}

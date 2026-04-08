package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(
        name = "edges",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"reactFlowId", "project_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Edge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // React Flow edge ID
    @Column(nullable = false)
    private String reactFlowId;

    // Source Node
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    private Node sourceNode;

    // Target Node
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private Node targetNode;

    // Additional JSON data from React Flow (type, data, markerEnd, style, animated)
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // Project reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
}

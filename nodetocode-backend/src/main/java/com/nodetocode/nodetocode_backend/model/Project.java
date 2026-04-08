package com.nodetocode.nodetocode_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Info
    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // 🚫 Prevent circular reference back to User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    // 🚫 Prevent deep nesting (Node.project → Project → Node → infinite)
    // cascade = REMOVE so deleting a project cascades to its nodes/edges
    // NO orphanRemoval — nodes/edges are managed via their own DAOs in GraphService
    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
    private List<Node> nodes;

    @JsonIgnore
    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE)
    private List<Edge> edges;

    // React Flow viewport state (saved/restored with graph)
    private Double viewportX;
    private Double viewportY;
    private Double viewportZoom;

    @Column(columnDefinition = "TEXT")
    private String generatedPrompt;

    @Column(columnDefinition = "TEXT")
    private String generatedCode;

    @Column(columnDefinition = "TEXT")
    private String userCode;

    @Enumerated(EnumType.STRING)
    private CodeLanguage language;

    private Long lastTokensUsed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
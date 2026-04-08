package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dsa_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "dsa_problem_id", "language"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DSASubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dsa_problem_id", nullable = false)
    private DSAProblem dsaProblem;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodeLanguage language;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @Builder.Default
    private Boolean solved = false;

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

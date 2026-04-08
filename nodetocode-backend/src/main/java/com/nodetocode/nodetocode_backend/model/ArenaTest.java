package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "arena_tests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ArenaTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArenaTestMode mode;

    /** Comma-separated DSAProblem IDs assigned to this test */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String problemIds;

    /** Duration in seconds allowed for this test */
    @Column(nullable = false)
    private Integer durationSeconds;

    /** When the user started the test */
    private LocalDateTime startedAt;

    /** When the user finished / time expired */
    private LocalDateTime finishedAt;

    /** Number of problems solved correctly */
    @Builder.Default
    private Integer solvedCount = 0;

    /** Total problems in this test */
    @Column(nullable = false)
    private Integer totalProblems;

    /** Calculated score (0–1000) */
    @Builder.Default
    private Integer score = 0;

    /** Time bonus points */
    @Builder.Default
    private Integer timeBonus = 0;

    /** Badge earned: NONE, BRONZE, SILVER, GOLD, PLATINUM */
    @Builder.Default
    private String badge = "NONE";

    /** Whether the test is still active (not yet submitted) */
    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.startedAt == null) this.startedAt = LocalDateTime.now();
    }
}

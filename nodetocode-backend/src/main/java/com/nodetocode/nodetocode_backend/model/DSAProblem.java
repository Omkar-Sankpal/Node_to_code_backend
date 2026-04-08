package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dsa_problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DSAProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String sampleInput;

    @Column(columnDefinition = "TEXT")
    private String sampleOutput;

    @ElementCollection
    @CollectionTable(name = "dsa_problem_tags", joinColumns = @JoinColumn(name = "dsa_problem_id"))
    @Column(name = "tag", nullable = false)
    private List<String> tags = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

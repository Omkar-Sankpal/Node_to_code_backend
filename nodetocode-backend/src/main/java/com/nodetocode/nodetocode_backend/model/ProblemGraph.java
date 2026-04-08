package com.nodetocode.nodetocode_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "problem_graphs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "problem_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProblemGraph {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    // React Flow viewport state
    private Double viewportX;
    private Double viewportY;
    private Double viewportZoom;
}

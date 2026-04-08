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
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    // 🔒 Never expose password in JSON
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private boolean verified = false;

    private String provider;      // LOCAL, GOOGLE
    private String providerId;

    @Column(nullable = false)
    private String role; // ROLE_USER, ROLE_GUEST

    @Column(nullable = false)
    private Long totalTokensUsed = 0L;

    @Column(length = 512)
    private String aiApiKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🚫 Prevent circular JSON
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects;

    // Auto timestamp handling
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
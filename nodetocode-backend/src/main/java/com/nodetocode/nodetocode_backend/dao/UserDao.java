// src/main/java/com/nodetocode/nodetocode_backend/dao/UserDao.java
package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserDao extends JpaRepository<User, Long> {

    // Find user by email (useful for login / OAuth)
    Optional<User> findByEmail(String email);

    // Find user by provider + providerId (useful for Google login)
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // Optional: check if email exists
    boolean existsByEmail(String email);

    // Clean old guest users :: I did this coz other methods would req more setup
    void deleteByRoleAndCreatedAtBefore(String role, LocalDateTime time);
}

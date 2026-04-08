package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.Project;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.model.CodeLanguage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDao extends JpaRepository<Project, Long> {

    // Get all projects of a specific user
    List<Project> findByUser(User user);

    // Get project by id and user (important for security)
    Optional<Project> findByIdAndUser(Long id, User user);

    // Find projects by language
    List<Project> findByLanguage(CodeLanguage language);

    // Find projects by title (search feature)
    List<Project> findByTitleContainingIgnoreCase(String title);

}
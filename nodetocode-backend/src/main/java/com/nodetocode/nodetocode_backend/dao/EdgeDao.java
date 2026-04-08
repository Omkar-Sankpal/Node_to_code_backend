package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.Edge;
import com.nodetocode.nodetocode_backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EdgeDao extends JpaRepository<Edge, Long> {

    List<Edge> findByProject(Project project);
}

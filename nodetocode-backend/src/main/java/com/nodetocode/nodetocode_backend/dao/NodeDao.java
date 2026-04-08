package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.Node;
import com.nodetocode.nodetocode_backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeDao extends JpaRepository<Node, Long> {

    List<Node> findByProject(Project project);

    Optional<Node> findByReactFlowIdAndProject(String reactFlowId, Project project);
}

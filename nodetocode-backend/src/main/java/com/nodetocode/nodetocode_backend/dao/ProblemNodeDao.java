package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.ProblemGraph;
import com.nodetocode.nodetocode_backend.model.ProblemNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemNodeDao extends JpaRepository<ProblemNode, Long> {
    List<ProblemNode> findByProblemGraph(ProblemGraph problemGraph);
    Optional<ProblemNode> findByReactFlowIdAndProblemGraph(String reactFlowId, ProblemGraph problemGraph);
}

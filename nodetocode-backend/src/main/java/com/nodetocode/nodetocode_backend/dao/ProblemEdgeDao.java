package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.ProblemEdge;
import com.nodetocode.nodetocode_backend.model.ProblemGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemEdgeDao extends JpaRepository<ProblemEdge, Long> {
    List<ProblemEdge> findByProblemGraph(ProblemGraph problemGraph);
}

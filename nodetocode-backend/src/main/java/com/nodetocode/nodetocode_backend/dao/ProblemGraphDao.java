package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.Problem;
import com.nodetocode.nodetocode_backend.model.ProblemGraph;
import com.nodetocode.nodetocode_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProblemGraphDao extends JpaRepository<ProblemGraph, Long> {
    Optional<ProblemGraph> findByUserAndProblem(User user, Problem problem);
}

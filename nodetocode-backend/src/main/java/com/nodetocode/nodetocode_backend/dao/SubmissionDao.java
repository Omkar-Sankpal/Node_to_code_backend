package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionDao extends JpaRepository<Submission, Long> {

    Optional<Submission> findByUserIdAndProblemIdAndLanguage(Long userId, Long problemId, CodeLanguage language);

    List<Submission> findByUserIdAndProblemId(Long userId, Long problemId);

    List<Submission> findByUserId(Long userId);

    @Query("SELECT DISTINCT s.problem.id FROM Submission s WHERE s.user.id = :userId AND s.solved = true")
    List<Long> findSolvedProblemIdsByUserId(@Param("userId") Long userId);
}

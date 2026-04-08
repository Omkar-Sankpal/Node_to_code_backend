package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.DSASubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DSASubmissionDao extends JpaRepository<DSASubmission, Long> {

    Optional<DSASubmission> findByUserIdAndDsaProblemIdAndLanguage(Long userId, Long dsaProblemId, CodeLanguage language);

    List<DSASubmission> findByUserIdAndDsaProblemId(Long userId, Long dsaProblemId);

    List<DSASubmission> findByUserId(Long userId);

    @Query("SELECT DISTINCT s.dsaProblem.id FROM DSASubmission s WHERE s.user.id = :userId AND s.solved = true")
    List<Long> findSolvedDsaProblemIdsByUserId(@Param("userId") Long userId);
}

package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.ArenaTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaTestDao extends JpaRepository<ArenaTest, Long> {

    List<ArenaTest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ArenaTest> findByIdAndUserId(Long id, Long userId);

    /** Find any currently active test for the user */
    Optional<ArenaTest> findByUserIdAndActiveTrue(Long userId);

    /** Count completed tests */
    long countByUserIdAndActiveFalse(Long userId);

    /** Sum of scores for a user */
    @Query("SELECT COALESCE(SUM(a.score), 0) FROM ArenaTest a WHERE a.user.id = :userId AND a.active = false")
    int sumScoreByUserId(@Param("userId") Long userId);

    /** Best score */
    @Query("SELECT COALESCE(MAX(a.score), 0) FROM ArenaTest a WHERE a.user.id = :userId AND a.active = false")
    int maxScoreByUserId(@Param("userId") Long userId);
}

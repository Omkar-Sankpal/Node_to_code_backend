package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.Problem;
import com.nodetocode.nodetocode_backend.model.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProblemDao extends JpaRepository<Problem, Long> {

    List<Problem> findByDifficulty(Difficulty difficulty);

    List<Problem> findByTitleContainingIgnoreCase(String keyword);
}
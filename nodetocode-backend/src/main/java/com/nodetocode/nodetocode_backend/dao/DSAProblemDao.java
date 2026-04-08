package com.nodetocode.nodetocode_backend.dao;

import com.nodetocode.nodetocode_backend.model.DSAProblem;
import com.nodetocode.nodetocode_backend.model.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DSAProblemDao extends JpaRepository<DSAProblem, Long> {

    List<DSAProblem> findByDifficulty(Difficulty difficulty);

    List<DSAProblem> findByTitleContainingIgnoreCase(String keyword);
}

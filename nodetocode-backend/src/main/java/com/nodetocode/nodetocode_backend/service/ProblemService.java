package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.Difficulty;
import com.nodetocode.nodetocode_backend.model.Problem;

import java.util.List;

public interface ProblemService {

    Problem createProblem(Problem problem);

    List<Problem> createProblems(List<Problem> problems);

    Problem updateProblem(Long id, Problem problem);

    void deleteProblem(Long id);

    Problem getProblemById(Long id);

    List<Problem> getAllProblems();

    List<Problem> getByDifficulty(Difficulty difficulty);

    List<Problem> searchByTitle(String keyword);
}
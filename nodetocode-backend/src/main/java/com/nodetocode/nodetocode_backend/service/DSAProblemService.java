package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.DSAProblem;
import com.nodetocode.nodetocode_backend.model.Difficulty;

import java.util.List;

public interface DSAProblemService {

    DSAProblem createProblem(DSAProblem problem);

    List<DSAProblem> createProblems(List<DSAProblem> problems);

    DSAProblem updateProblem(Long id, DSAProblem problem);

    void deleteProblem(Long id);

    DSAProblem getProblemById(Long id);

    List<DSAProblem> getAllProblems();

    List<DSAProblem> getByDifficulty(Difficulty difficulty);

    List<DSAProblem> searchByTitle(String keyword);
}

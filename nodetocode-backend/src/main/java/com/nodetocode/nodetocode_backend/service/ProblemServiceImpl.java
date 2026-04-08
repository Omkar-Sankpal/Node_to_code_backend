package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.Difficulty;
import com.nodetocode.nodetocode_backend.model.Problem;
import com.nodetocode.nodetocode_backend.dao.ProblemDao;
import com.nodetocode.nodetocode_backend.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemDao problemDao;

    @Override
    public Problem createProblem(Problem problem) {
        return problemDao.save(problem);
    }

    @Override
    public List<Problem> createProblems(List<Problem> problems) {
        return problemDao.saveAll(problems);
    }

    @Override
    public Problem updateProblem(Long id, Problem updated) {
        Problem existing = problemDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setDifficulty(updated.getDifficulty());
        existing.setSampleInput(updated.getSampleInput());
        existing.setSampleOutput(updated.getSampleOutput());

        return problemDao.save(existing);
    }

    @Override
    public void deleteProblem(Long id) {
        problemDao.deleteById(id);
    }

    @Override
    public Problem getProblemById(Long id) {
        return problemDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

    @Override
    public List<Problem> getAllProblems() {
        return problemDao.findAll();
    }

    @Override
    public List<Problem> getByDifficulty(Difficulty difficulty) {
        return problemDao.findByDifficulty(difficulty);
    }

    @Override
    public List<Problem> searchByTitle(String keyword) {
        return problemDao.findByTitleContainingIgnoreCase(keyword);
    }
}
package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.DSAProblemDao;
import com.nodetocode.nodetocode_backend.model.DSAProblem;
import com.nodetocode.nodetocode_backend.model.Difficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DSAProblemServiceImpl implements DSAProblemService {

    private final DSAProblemDao dsaProblemDao;

    @Override
    public DSAProblem createProblem(DSAProblem problem) {
        return dsaProblemDao.save(problem);
    }

    @Override
    public List<DSAProblem> createProblems(List<DSAProblem> problems) {
        return dsaProblemDao.saveAll(problems);
    }

    @Override
    public DSAProblem updateProblem(Long id, DSAProblem updated) {
        DSAProblem existing = dsaProblemDao.findById(id)
                .orElseThrow(() -> new RuntimeException("DSA Problem not found"));

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setDifficulty(updated.getDifficulty());
        existing.setSampleInput(updated.getSampleInput());
        existing.setSampleOutput(updated.getSampleOutput());
        existing.setTags(updated.getTags());

        return dsaProblemDao.save(existing);
    }

    @Override
    public void deleteProblem(Long id) {
        dsaProblemDao.deleteById(id);
    }

    @Override
    public DSAProblem getProblemById(Long id) {
        return dsaProblemDao.findById(id)
                .orElseThrow(() -> new RuntimeException("DSA Problem not found"));
    }

    @Override
    public List<DSAProblem> getAllProblems() {
        return dsaProblemDao.findAll();
    }

    @Override
    public List<DSAProblem> getByDifficulty(Difficulty difficulty) {
        return dsaProblemDao.findByDifficulty(difficulty);
    }

    @Override
    public List<DSAProblem> searchByTitle(String keyword) {
        return dsaProblemDao.findByTitleContainingIgnoreCase(keyword);
    }
}

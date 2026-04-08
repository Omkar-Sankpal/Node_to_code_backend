package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.Difficulty;
import com.nodetocode.nodetocode_backend.model.DSAProblem;
import com.nodetocode.nodetocode_backend.service.DSAProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dsa-problems")
@RequiredArgsConstructor
public class DSAProblemController {

    private final DSAProblemService dsaProblemService;

    @PostMapping
    public DSAProblem create(@Valid @RequestBody DSAProblem problem) {
        return dsaProblemService.createProblem(problem);
    }

    @PostMapping("/batch")
    public List<DSAProblem> createBatch(@Valid @RequestBody List<DSAProblem> problems) {
        return dsaProblemService.createProblems(problems);
    }

    @PutMapping("/{id}")
    public DSAProblem update(@PathVariable Long id,
                             @Valid @RequestBody DSAProblem problem) {
        return dsaProblemService.updateProblem(id, problem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        dsaProblemService.deleteProblem(id);
    }

    @GetMapping("/{id}")
    public DSAProblem getById(@PathVariable Long id) {
        return dsaProblemService.getProblemById(id);
    }

    @GetMapping
    public List<DSAProblem> getAll() {
        return dsaProblemService.getAllProblems();
    }

    @GetMapping("/difficulty/{difficulty}")
    public List<DSAProblem> getByDifficulty(@PathVariable Difficulty difficulty) {
        return dsaProblemService.getByDifficulty(difficulty);
    }

    @GetMapping("/search")
    public List<DSAProblem> search(@RequestParam String keyword) {
        return dsaProblemService.searchByTitle(keyword);
    }
}

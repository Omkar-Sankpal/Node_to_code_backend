package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.Difficulty;
import com.nodetocode.nodetocode_backend.model.Problem;
import com.nodetocode.nodetocode_backend.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    public Problem create(@Valid @RequestBody Problem problem) {
        return problemService.createProblem(problem);
    }

    @PostMapping("/batch")
    public List<Problem> createBatch(@Valid @RequestBody List<Problem> problems) {
        return problemService.createProblems(problems);
    }

    @PutMapping("/{id}")
    public Problem update(@PathVariable Long id,
                          @Valid @RequestBody Problem problem) {
        return problemService.updateProblem(id, problem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        problemService.deleteProblem(id);
    }

    @GetMapping("/{id}")
    public Problem getById(@PathVariable Long id) {
        return problemService.getProblemById(id);
    }

    @GetMapping
    public List<Problem> getAll() {
        return problemService.getAllProblems();
    }

    @GetMapping("/difficulty/{difficulty}")
    public List<Problem> getByDifficulty(@PathVariable Difficulty difficulty) {
        return problemService.getByDifficulty(difficulty);
    }

    @GetMapping("/search")
    public List<Problem> search(@RequestParam String keyword) {
        return problemService.searchByTitle(keyword);
    }
}
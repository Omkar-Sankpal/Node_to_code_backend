package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.model.Project;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.service.ProjectService;
import com.nodetocode.nodetocode_backend.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    public ProjectController(ProjectService projectService,
                             UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    // Create project
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        User currentUser = userService.getCurrentUser();
        Project created = projectService.createProject(project, currentUser);
        return ResponseEntity.ok(created);
    }

    // Get all user projects
    @GetMapping
    public ResponseEntity<List<Project>> getUserProjects() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(projectService.getUserProjects(currentUser));
    }

    // Get single project
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        return projectService.getProjectById(id, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update project
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Long id,
                                                 @RequestBody Project project) {
        User currentUser = userService.getCurrentUser();
        return projectService.updateProject(id, project, currentUser)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete project
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        boolean deleted = projectService.deleteProject(id, currentUser);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Save user code for a project
    @PatchMapping("/{id}/code")
    public ResponseEntity<?> saveCode(@PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        User currentUser = userService.getCurrentUser();
        String code = body.get("code");
        if (code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }
        return projectService.saveUserCode(id, code, currentUser)
                .map(p -> ResponseEntity.ok(Map.of(
                        "id", p.getId(),
                        "userCode", p.getUserCode() != null ? p.getUserCode() : "",
                        "updatedAt", p.getUpdatedAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // Save generated prompt for a project
    @PatchMapping("/{id}/prompt")
    public ResponseEntity<?> saveGeneratedPrompt(@PathVariable Long id,
                                                  @RequestBody Map<String, String> body) {
        User currentUser = userService.getCurrentUser();
        String prompt = body.get("prompt");
        if (prompt == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt is required"));
        }
        return projectService.saveGeneratedPrompt(id, prompt, currentUser)
                .map(p -> ResponseEntity.ok(Map.of(
                        "id", p.getId(),
                        "updatedAt", p.getUpdatedAt().toString()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
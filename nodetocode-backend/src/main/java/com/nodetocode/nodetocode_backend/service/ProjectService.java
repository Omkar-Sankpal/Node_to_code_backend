package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.ProjectDao;
import com.nodetocode.nodetocode_backend.model.Project;
import com.nodetocode.nodetocode_backend.model.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectDao projectDao;

    public ProjectService(ProjectDao projectDao) {
        this.projectDao = projectDao;
    }

    // Create new project
    @Transactional
    public Project createProject(Project project, User user) {
        project.setUser(user);
        return projectDao.save(project);
    }

    // Get all projects of logged in user
    public List<Project> getUserProjects(User user) {
        return projectDao.findByUser(user);
    }

    // Get single project (security safe)
    public Optional<Project> getProjectById(Long id, User user) {
        return projectDao.findByIdAndUser(id, user);
    }

    // Update project
    @Transactional
    public Optional<Project> updateProject(Long id, Project updatedProject, User user) {
        return projectDao.findByIdAndUser(id, user).map(project -> {
            project.setTitle(updatedProject.getTitle());
            project.setDescription(updatedProject.getDescription());
            project.setLanguage(updatedProject.getLanguage());
            return projectDao.save(project);
        });
    }

    // Delete project
    @Transactional
    public boolean deleteProject(Long id, User user) {
        Optional<Project> project = projectDao.findByIdAndUser(id, user);
        if (project.isPresent()) {
            projectDao.delete(project.get());
            return true;
        }
        return false;
    }

    // Save user code for a project
    @Transactional
    public Optional<Project> saveUserCode(Long id, String code, User user) {
        return projectDao.findByIdAndUser(id, user).map(project -> {
            project.setUserCode(code);
            return projectDao.save(project);
        });
    }

    // Save generated prompt for a project
    @Transactional
    public Optional<Project> saveGeneratedPrompt(Long id, String prompt, User user) {
        return projectDao.findByIdAndUser(id, user).map(project -> {
            project.setGeneratedPrompt(prompt);
            return projectDao.save(project);
        });
    }
}
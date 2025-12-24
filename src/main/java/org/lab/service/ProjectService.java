package org.lab.service;

import org.lab.domain.DomainException;
import org.lab.domain.Project;
import org.lab.domain.ProjectRole;
import org.lab.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projectRepo;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepo, UserService userService) {
        this.projectRepo = projectRepo;
        this.userService = userService;
    }

    public Project create(String name, String managerLogin) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Project name is missing");
        }
        userService.requireExists(managerLogin);

        Project project = new Project(UUID.randomUUID(), name.trim(), managerLogin, null, Set.of(), Set.of());
        projectRepo.save(project);
        return project;
    }

    public Project getById(UUID id) {
        return projectRepo.findById(id)
                .orElseThrow(() -> new DomainException("Project not found: " + id));
    }

    public List<Project> getByUser(String userLogin) {
        return projectRepo.findByUserLogin(userLogin);
    }

    public ProjectRole getRoleInProject(UUID projectId, String userLogin) {
        return projectRepo.getRoleInProject(projectId, userLogin);
    }

    public Project addMember(UUID projectId, String userLogin, ProjectRole role, String actorLogin, ProjectRole actorRole) {
        requireManager(projectId, actorLogin, actorRole);
        userService.requireExists(userLogin);

        Project project = getById(projectId);

        return switch (role) {
            case TEAM_LEADER -> {
                Project updated = new Project(project.id(), project.name(), project.managerLogin(),
                        userLogin, project.developerLogins(), project.testerLogins());
                projectRepo.save(updated);
                yield updated;
            }
            case DEVELOPER -> {
                LinkedHashSet<String> devs = new LinkedHashSet<>(project.developerLogins());
                devs.add(userLogin);
                Project updated = new Project(project.id(), project.name(), project.managerLogin(),
                        project.teamLeaderLogin(), devs, project.testerLogins());
                projectRepo.save(updated);
                yield updated;
            }
            case TESTER -> {
                LinkedHashSet<String> testers = new LinkedHashSet<>(project.testerLogins());
                testers.add(userLogin);
                Project updated = new Project(project.id(), project.name(), project.managerLogin(),
                        project.teamLeaderLogin(), project.developerLogins(), testers);
                projectRepo.save(updated);
                yield updated;
            }
            case MANAGER -> throw new DomainException("Cannot add another manager");
        };
    }

    public void requireManager(UUID projectId, String actorLogin, ProjectRole actorRole) {
        if (actorRole != ProjectRole.MANAGER) {
            throw new DomainException("Only project manager can do this");
        }
        Project project = getById(projectId);
        if (!project.managerLogin().equals(actorLogin)) {
            throw new DomainException("Only project manager can do this");
        }
    }

    public void requireManagerOrLead(UUID projectId, String actorLogin, ProjectRole actorRole) {
        if (actorRole != ProjectRole.MANAGER && actorRole != ProjectRole.TEAM_LEADER) {
            throw new DomainException("Only manager/team leader can do this");
        }
    }
}



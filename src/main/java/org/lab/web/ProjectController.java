package org.lab.web;

import org.lab.domain.Project;
import org.lab.domain.ProjectRole;
import org.lab.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    public record CreateProjectRequest(String name) {}
    public record AddMemberRequest(String userLogin, ProjectRole role) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project create(@RequestBody CreateProjectRequest request,
                          @RequestParam String user) {
        return projectService.create(request.name(), user);
    }

    @GetMapping
    public List<Project> getMyProjects(@RequestParam String user) {
        return projectService.getByUser(user);
    }

    @GetMapping("/{id}")
    public Project getById(@PathVariable UUID id) {
        return projectService.getById(id);
    }

    @PostMapping("/{id}/members")
    public Project addMember(@PathVariable UUID id,
                             @RequestBody AddMemberRequest request,
                             @RequestParam String user,
                             @RequestParam ProjectRole role) {
        return projectService.addMember(id, request.userLogin(), request.role(), user, role);
    }
}



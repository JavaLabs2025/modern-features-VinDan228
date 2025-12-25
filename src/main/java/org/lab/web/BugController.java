package org.lab.web;

import org.lab.domain.BugReport;
import org.lab.domain.BugStatus;
import org.lab.domain.ProjectRole;
import org.lab.service.BugService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class BugController {
    private final BugService bugService;

    public BugController(BugService bugService) {
        this.bugService = bugService;
    }

    public record CreateBugRequest(UUID projectId, String title) {}
    public record UpdateBugRequest(String assigneeLogin, BugStatus status) {}

    @PostMapping("/bugs")
    @ResponseStatus(HttpStatus.CREATED)
    public BugReport create(@RequestBody CreateBugRequest request,
                            @RequestParam String user,
                            @RequestParam ProjectRole role) {
        return bugService.create(request.projectId(), request.title(), user, role);
    }

    @GetMapping("/projects/{projectId}/bugs")
    public List<BugReport> getByProject(@PathVariable UUID projectId) {
        return bugService.getByProjectId(projectId);
    }

    @GetMapping("/bugs/{id}")
    public BugReport getById(@PathVariable UUID id) {
        return bugService.getById(id);
    }

    @PatchMapping("/bugs/{id}")
    public BugReport update(@PathVariable UUID id,
                            @RequestBody UpdateBugRequest request,
                            @RequestParam String user,
                            @RequestParam ProjectRole role) {
        BugReport result = bugService.getById(id);
        result = request.assigneeLogin() != null 
                ? bugService.assign(id, request.assigneeLogin(), user, role) 
                : result;
        return request.status() != null 
                ? bugService.setStatus(id, request.status(), user, role) 
                : result;
    }

    @GetMapping("/my/bugs")
    public List<BugReport> myBugs(@RequestParam String user,
                                  @RequestParam ProjectRole role) {
        return bugService.getMyBugs(user, role);
    }
}



package org.lab.web;

import org.lab.domain.Milestone;
import org.lab.domain.ProjectRole;
import org.lab.service.MilestoneService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class MilestoneController {
    private final MilestoneService milestoneService;

    public MilestoneController(MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }

    public record CreateMilestoneRequest(String name, LocalDate startDate, LocalDate endDate) {}
    public record UpdateStatusRequest(String action) {}

    @GetMapping("/projects/{projectId}/milestones")
    public List<Milestone> getByProject(@PathVariable UUID projectId) {
        return milestoneService.getByProjectId(projectId);
    }

    @PostMapping("/projects/{projectId}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    public Milestone create(@PathVariable UUID projectId,
                            @RequestBody CreateMilestoneRequest request,
                            @RequestParam String user,
                            @RequestParam ProjectRole role) {
        return milestoneService.create(projectId, request.name(), request.startDate(), request.endDate(), user, role);
    }

    @GetMapping("/milestones/{id}")
    public Milestone getById(@PathVariable UUID id) {
        return milestoneService.getById(id);
    }

    @PatchMapping("/milestones/{id}")
    public Milestone updateStatus(@PathVariable UUID id,
                                  @RequestBody UpdateStatusRequest request,
                                  @RequestParam String user,
                                  @RequestParam ProjectRole role) {
        return switch (request.action().toLowerCase()) {
            case "activate" -> milestoneService.activate(id, user, role);
            case "close" -> milestoneService.close(id, user, role);
            default -> throw new IllegalArgumentException("Unknown action: " + request.action());
        };
    }
}



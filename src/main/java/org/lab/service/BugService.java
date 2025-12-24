package org.lab.service;

import org.lab.domain.*;
import org.lab.repository.BugRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BugService {
    private final BugRepository bugRepo;
    private final ProjectService projectService;
    private final UserService userService;

    public BugService(BugRepository bugRepo, ProjectService projectService, UserService userService) {
        this.bugRepo = bugRepo;
        this.projectService = projectService;
        this.userService = userService;
    }

    public BugReport create(UUID projectId, String title, String reporterLogin, ProjectRole reporterRole) {
        if (reporterRole != ProjectRole.DEVELOPER && reporterRole != ProjectRole.TESTER && reporterRole != ProjectRole.TEAM_LEADER) {
            throw new DomainException("Only developer/tester/team lead can create bug reports");
        }
        if (title == null || title.isBlank()) {
            throw new DomainException("Bug title is missing");
        }

        BugReport bug = new BugReport(UUID.randomUUID(), projectId, title.trim(), reporterLogin, null, BugStatus.NEW);
        bugRepo.save(bug);
        return bug;
    }

    public BugReport getById(UUID id) {
        return bugRepo.findById(id)
                .orElseThrow(() -> new DomainException("Bug not found: " + id));
    }

    public List<BugReport> getByProjectId(UUID projectId) {
        return bugRepo.findByProjectId(projectId);
    }

    public List<BugReport> getMyBugs(String userLogin, ProjectRole role) {
        List<BugReport> result = new ArrayList<>();

        // Bugs assigned to me (as developer)
        if (role == ProjectRole.DEVELOPER || role == ProjectRole.TEAM_LEADER) {
            result.addAll(bugRepo.findByAssignee(userLogin));
        }

        return result;
    }

    public List<BugReport> getNeedsTesting(UUID projectId) {
        return bugRepo.findNeedsTesting(projectId);
    }

    public BugReport assign(UUID bugId, String assigneeLogin, String actorLogin, ProjectRole actorRole) {
        BugReport b = getById(bugId);

        if (b.status() == BugStatus.CLOSED) {
            throw new DomainException("Cannot assign bug: already CLOSED");
        }

        boolean canAssign = actorRole == ProjectRole.MANAGER
                || actorRole == ProjectRole.TEAM_LEADER
                || (actorRole == ProjectRole.DEVELOPER && actorLogin.equals(assigneeLogin));

        if (!canAssign) {
            throw new DomainException("Not allowed to assign bug");
        }

        userService.requireExists(assigneeLogin);
        Project project = projectService.getById(b.projectId());
        if (!project.developerLogins().contains(assigneeLogin) &&
            !(project.teamLeaderLogin() != null && project.teamLeaderLogin().equals(assigneeLogin))) {
            throw new DomainException("User is not a developer in this project: " + assigneeLogin);
        }

        BugReport updated = new BugReport(b.id(), b.projectId(), b.title(), b.reporterLogin(), assigneeLogin, b.status());
        bugRepo.save(updated);
        return updated;
    }

    public BugReport setStatus(UUID bugId, BugStatus newStatus, String actorLogin, ProjectRole actorRole) {
        BugReport b = getById(bugId);

        if (!isAllowedTransition(b.status(), newStatus)) {
            throw new DomainException("Invalid bug status transition: " + b.status() + " -> " + newStatus);
        }

        boolean canChange = switch (newStatus) {
            case FIXED -> {
                if (b.assigneeDeveloperLogin() == null) {
                    throw new DomainException("Bug must be assigned before marking as FIXED");
                }
                yield actorLogin.equals(b.assigneeDeveloperLogin())
                        || actorRole == ProjectRole.MANAGER
                        || actorRole == ProjectRole.TEAM_LEADER;
            }
            case TESTED, CLOSED -> actorRole == ProjectRole.TESTER
                    || actorRole == ProjectRole.MANAGER
                    || actorRole == ProjectRole.TEAM_LEADER;
            case NEW -> actorRole == ProjectRole.MANAGER || actorRole == ProjectRole.TEAM_LEADER;
        };

        if (!canChange) {
            throw new DomainException("Not allowed to change bug status to " + newStatus);
        }

        BugReport updated = new BugReport(b.id(), b.projectId(), b.title(), b.reporterLogin(), b.assigneeDeveloperLogin(), newStatus);
        bugRepo.save(updated);
        return updated;
    }

    private boolean isAllowedTransition(BugStatus from, BugStatus to) {
        return switch (from) {
            case NEW -> to == BugStatus.FIXED;
            case FIXED -> to == BugStatus.TESTED;
            case TESTED -> to == BugStatus.CLOSED;
            case CLOSED -> false;
        };
    }
}



package org.lab.service;

import jakarta.annotation.Nonnull;
import org.lab.domain.*;
import org.lab.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TicketService {
    private final TicketRepository ticketRepo;
    private final ProjectService projectService;
    private final MilestoneService milestoneService;
    private final UserService userService;

    public TicketService(TicketRepository ticketRepo, ProjectService projectService,
                         MilestoneService milestoneService, UserService userService) {
        this.ticketRepo = ticketRepo;
        this.projectService = projectService;
        this.milestoneService = milestoneService;
        this.userService = userService;
    }

    public Ticket create(UUID projectId, UUID milestoneId, String title,
                         String actorLogin, ProjectRole actorRole) {
        projectService.requireManagerOrLead(projectId, actorLogin, actorRole);

        Milestone ms = milestoneService.getById(milestoneId);
        if (!ms.projectId().equals(projectId)) {
            throw new DomainException("Milestone does not belong to project");
        }
        if (ms.status() == MilestoneStatus.CLOSED) {
            throw new DomainException("Milestone is CLOSED");
        }
        if (title == null || title.isBlank()) {
            throw new DomainException("Ticket title must not be blank");
        }

        Ticket ticket = new Ticket(UUID.randomUUID(), projectId, milestoneId, title.trim(), Set.of(), TicketStatus.NEW);
        ticketRepo.save(ticket);
        return ticket;
    }

    public Ticket getById(UUID id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new DomainException("Ticket not found: " + id));
    }

    public List<Ticket> getByMilestoneId(UUID milestoneId) {
        return ticketRepo.findByMilestoneId(milestoneId);
    }

    public List<Ticket> getByAssignee(String userLogin) {
        return ticketRepo.findByAssignee(userLogin);
    }

    public Ticket assign(UUID ticketId, String assigneeLogin, String actorLogin, ProjectRole actorRole) {
        Ticket t = getById(ticketId);
        projectService.requireManagerOrLead(t.projectId(), actorLogin, actorRole);

        Milestone ms = milestoneService.getById(t.milestoneId());
        if (ms.status() == MilestoneStatus.CLOSED) {
            throw new DomainException("Cannot assign ticket: milestone is CLOSED");
        }

        userService.requireExists(assigneeLogin);
        Project project = projectService.getById(t.projectId());
        Ticket updated = getTicket(assigneeLogin, project, t);
        ticketRepo.save(updated);
        return updated;
    }

    @Nonnull
    private static Ticket getTicket(String assigneeLogin, Project project, Ticket t) {
        if (!project.developerLogins().contains(assigneeLogin) &&
            !(project.teamLeaderLogin() != null && project.teamLeaderLogin().equals(assigneeLogin))) {
            throw new DomainException("User is not a developer/lead in this project: " + assigneeLogin);
        }

        LinkedHashSet<String> assignees = new LinkedHashSet<>(t.assigneeLogins());
        assignees.add(assigneeLogin);
        return new Ticket(t.id(), t.projectId(), t.milestoneId(), t.title(), assignees, t.status());
    }

    public Ticket setStatus(UUID ticketId, TicketStatus newStatus, String actorLogin, ProjectRole actorRole) {
        Ticket t = getById(ticketId);

        if (!isAllowedTransition(t.status(), newStatus)) {
            throw new DomainException("Invalid ticket status transition: " + t.status() + " -> " + newStatus);
        }

        Milestone ms = milestoneService.getById(t.milestoneId());
        if (ms.status() == MilestoneStatus.CLOSED) {
            throw new DomainException("Cannot change ticket: milestone is CLOSED");
        }

        boolean canChange = actorRole == ProjectRole.MANAGER
                || actorRole == ProjectRole.TEAM_LEADER
                || (actorRole == ProjectRole.DEVELOPER && t.isAssignedTo(actorLogin));

        if (!canChange) {
            throw new DomainException("Not allowed to change ticket status");
        }

        Ticket updated = new Ticket(t.id(), t.projectId(), t.milestoneId(), t.title(), t.assigneeLogins(), newStatus);
        ticketRepo.save(updated);
        return updated;
    }

    private boolean isAllowedTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case NEW -> to == TicketStatus.ACCEPTED;
            case ACCEPTED -> to == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == TicketStatus.DONE;
            case DONE -> false;
        };
    }
}



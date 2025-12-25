package org.lab.service;

import org.lab.domain.DomainException;
import org.lab.domain.Milestone;
import org.lab.domain.MilestoneStatus;
import org.lab.domain.ProjectRole;
import org.lab.repository.MilestoneRepository;
import org.lab.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MilestoneService {
    private final MilestoneRepository milestoneRepo;
    private final TicketRepository ticketRepo;
    private final ProjectService projectService;

    public MilestoneService(MilestoneRepository milestoneRepo, TicketRepository ticketRepo, ProjectService projectService) {
        this.milestoneRepo = milestoneRepo;
        this.ticketRepo = ticketRepo;
        this.projectService = projectService;
    }

    public Milestone create(UUID projectId, String name, LocalDate startDate, LocalDate endDate,
                            String actorLogin, ProjectRole actorRole) {
        projectService.requireManager(projectId, actorLogin, actorRole);

        if (name == null || name.isBlank()) {
            throw new DomainException("Milestone name is missing");
        }
        if (milestoneRepo.hasCurrentMilestone(projectId)) {
            throw new DomainException("Project already has a current (OPEN/ACTIVE) milestone, close it first");
        }

        Milestone milestone = new Milestone(UUID.randomUUID(), projectId, name.trim(), startDate, endDate, MilestoneStatus.OPEN);
        milestoneRepo.save(milestone);
        return milestone;
    }

    public Milestone getById(UUID id) {
        return milestoneRepo.findById(id)
                .orElseThrow(() -> new DomainException("Milestone not found: " + id));
    }

    public List<Milestone> getByProjectId(UUID projectId) {
        return milestoneRepo.findByProjectId(projectId);
    }

    public Milestone activate(UUID milestoneId, String actorLogin, ProjectRole actorRole) {
        Milestone ms = getById(milestoneId);
        projectService.requireManager(ms.projectId(), actorLogin, actorRole);

        if (ms.status() == MilestoneStatus.CLOSED) {
            throw new DomainException("Milestone is CLOSED");
        }
        if (milestoneRepo.hasActiveMilestone(ms.projectId(), milestoneId)) {
            throw new DomainException("Project already has an ACTIVE milestone");
        }

        return ms.status() == MilestoneStatus.ACTIVE 
                ? ms 
                : updateMilestoneStatus(ms, MilestoneStatus.ACTIVE);
    }

    private Milestone updateMilestoneStatus(Milestone ms, MilestoneStatus newStatus) {
        Milestone updated = new Milestone(ms.id(), ms.projectId(), ms.name(), ms.startDate(), ms.endDate(), newStatus);
        milestoneRepo.save(updated);
        return updated;
    }

    public Milestone close(UUID milestoneId, String actorLogin, ProjectRole actorRole) {
        Milestone ms = getById(milestoneId);
        projectService.requireManager(ms.projectId(), actorLogin, actorRole);

        if (ms.status() != MilestoneStatus.ACTIVE) {
            throw new DomainException("Only ACTIVE milestone can be closed");
        }
        if (!ticketRepo.allTicketsDone(milestoneId)) {
            throw new DomainException("Cannot close milestone, not all tickets are DONE");
        }

        return updateMilestoneStatus(ms, MilestoneStatus.CLOSED);
    }
}



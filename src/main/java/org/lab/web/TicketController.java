package org.lab.web;

import org.lab.domain.ProjectRole;
import org.lab.domain.Ticket;
import org.lab.domain.TicketStatus;
import org.lab.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public record CreateTicketRequest(UUID projectId, UUID milestoneId, String title) {}
    public record UpdateTicketRequest(String assigneeLogin, TicketStatus status) {}

    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket create(@RequestBody CreateTicketRequest request,
                         @RequestParam String user,
                         @RequestParam ProjectRole role) {
        return ticketService.create(request.projectId(), request.milestoneId(), request.title(), user, role);
    }

    @GetMapping("/milestones/{milestoneId}/tickets")
    public List<Ticket> getByMilestone(@PathVariable UUID milestoneId) {
        return ticketService.getByMilestoneId(milestoneId);
    }

    @GetMapping("/tickets/{id}")
    public Ticket getById(@PathVariable UUID id) {
        return ticketService.getById(id);
    }

    @PatchMapping("/tickets/{id}")
    public Ticket update(@PathVariable UUID id,
                         @RequestBody UpdateTicketRequest request,
                         @RequestParam String user,
                         @RequestParam ProjectRole role) {
        Ticket result = ticketService.getById(id);
        result = request.assigneeLogin() != null 
                ? ticketService.assign(id, request.assigneeLogin(), user, role) 
                : result;
        return request.status() != null 
                ? ticketService.setStatus(id, request.status(), user, role) 
                : result;
    }

    @GetMapping("/my/tickets")
    public List<Ticket> myTickets(@RequestParam String user) {
        return ticketService.getByAssignee(user);
    }
}



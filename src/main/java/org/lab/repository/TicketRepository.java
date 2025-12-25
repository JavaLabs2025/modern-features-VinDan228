package org.lab.repository;

import org.lab.domain.Ticket;
import org.lab.domain.TicketStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class TicketRepository {
    private final JdbcTemplate jdbc;

    public TicketRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Ticket t) {
        jdbc.update("""
            INSERT INTO tickets (id, project_id, milestone_id, title, status)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                status = EXCLUDED.status
            """, t.id(), t.projectId(), t.milestoneId(), t.title(), t.status().name());

        jdbc.update("DELETE FROM ticket_assignees WHERE ticket_id = ?", t.id());
        t.assigneeLogins().stream()
                .forEach(login -> jdbc.update(
                        "INSERT INTO ticket_assignees (ticket_id, user_login) VALUES (?, ?)",
                        t.id(), login));
    }

    public Optional<Ticket> findById(UUID id) {
        return jdbc.queryForList(
                        "SELECT id, project_id, milestone_id, title, status FROM tickets WHERE id = ?", id)
                .stream()
                .findFirst()
                .map(this::buildTicket);
    }

    public List<Ticket> findByMilestoneId(UUID milestoneId) {
        return jdbc.queryForList(
                        "SELECT id, project_id, milestone_id, title, status FROM tickets WHERE milestone_id = ? ORDER BY title",
                        milestoneId)
                .stream()
                .map(this::buildTicket)
                .toList();
    }

    public List<Ticket> findByAssignee(String userLogin) {
        return jdbc.queryForList("""
            SELECT t.id, t.project_id, t.milestone_id, t.title, t.status
            FROM tickets t
            JOIN ticket_assignees ta ON ta.ticket_id = t.id
            WHERE ta.user_login = ?
            ORDER BY t.title
            """, userLogin)
                .stream()
                .map(this::buildTicket)
                .toList();
    }

    public boolean allTicketsDone(UUID milestoneId) {
        return Optional.ofNullable(
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM tickets WHERE milestone_id = ? AND status != 'DONE'",
                        Integer.class, milestoneId)
        ).map(count -> count == 0).orElse(true);
    }

    private Ticket buildTicket(Map<String, Object> row) {
        UUID id = (UUID) row.get("id");
        UUID projectId = (UUID) row.get("project_id");
        UUID milestoneId = (UUID) row.get("milestone_id");
        String title = (String) row.get("title");
        TicketStatus status = TicketStatus.valueOf((String) row.get("status"));

        Set<String> assignees = jdbc.queryForList(
                        "SELECT user_login FROM ticket_assignees WHERE ticket_id = ?", String.class, id)
                .stream()
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        return new Ticket(id, projectId, milestoneId, title, assignees, status);
    }
}

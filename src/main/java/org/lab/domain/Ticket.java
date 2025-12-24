package org.lab.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record Ticket(
        UUID id,
        UUID projectId,
        UUID milestoneId,
        String title,
        Set<String> assigneeLogins,
        TicketStatus status
) {
    public Ticket {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }

        if (milestoneId == null) {
            throw new IllegalArgumentException("milestoneId must not be null");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        if (assigneeLogins == null) {
            assigneeLogins = new LinkedHashSet<>();
        } else {
            assigneeLogins = new LinkedHashSet<>(assigneeLogins);
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    public boolean isAssignedTo(String login) {
        return assigneeLogins.contains(login);
    }
}

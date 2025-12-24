package org.lab.domain;

import java.time.LocalDate;
import java.util.UUID;

public record Milestone(
        UUID id,
        UUID projectId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        MilestoneStatus status
) {
    public Milestone {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("dates must not be null");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be >= startDate");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }
}

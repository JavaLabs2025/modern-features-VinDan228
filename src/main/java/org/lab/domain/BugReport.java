package org.lab.domain;

import java.util.UUID;

public record BugReport(
        UUID id,
        UUID projectId,
        String title,
        String reporterLogin,
        String assigneeDeveloperLogin,
        BugStatus status
) {
    public BugReport {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        if (reporterLogin == null || reporterLogin.isBlank()) {
            throw new IllegalArgumentException("reporterLogin must not be blank");
        }

        if (assigneeDeveloperLogin != null && assigneeDeveloperLogin.isBlank()) {
            assigneeDeveloperLogin = null;
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }
}

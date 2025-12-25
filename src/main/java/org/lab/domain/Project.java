package org.lab.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public record Project(
        UUID id,
        String name,
        String managerLogin,
        String teamLeaderLogin,
        Set<String> developerLogins,
        Set<String> testerLogins
) {
    public Project {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (managerLogin == null || managerLogin.isBlank()) {
            throw new IllegalArgumentException("managerLogin must not be blank");
        }

        developerLogins = developerLogins == null 
                ? new LinkedHashSet<>() 
                : new LinkedHashSet<>(developerLogins);

        testerLogins = testerLogins == null 
                ? new LinkedHashSet<>() 
                : new LinkedHashSet<>(testerLogins);

        teamLeaderLogin = (teamLeaderLogin != null && teamLeaderLogin.isBlank()) 
                ? null 
                : teamLeaderLogin;
    }

    public boolean participates(String login) {
        return managerLogin.equals(login)
                || (teamLeaderLogin != null && teamLeaderLogin.equals(login))
                || developerLogins.contains(login)
                || testerLogins.contains(login);
    }
}

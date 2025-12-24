package org.lab.repository;

import org.lab.domain.Project;
import org.lab.domain.ProjectRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProjectRepository {
    private final JdbcTemplate jdbc;

    public ProjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Project project) {
        jdbc.update("""
            INSERT INTO projects (id, name, manager_login) VALUES (?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, manager_login = EXCLUDED.manager_login
            """, project.id(), project.name(), project.managerLogin());

        // Clear existing members and re-insert
        jdbc.update("DELETE FROM project_members WHERE project_id = ?", project.id());

        if (project.teamLeaderLogin() != null) {
            jdbc.update("INSERT INTO project_members (project_id, user_login, role) VALUES (?, ?, ?)",
                    project.id(), project.teamLeaderLogin(), "TEAM_LEADER");
        }
        for (String dev : project.developerLogins()) {
            jdbc.update("INSERT INTO project_members (project_id, user_login, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    project.id(), dev, "DEVELOPER");
        }
        for (String tester : project.testerLogins()) {
            jdbc.update("INSERT INTO project_members (project_id, user_login, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                    project.id(), tester, "TESTER");
        }
    }

    public Optional<Project> findById(UUID id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name, manager_login FROM projects WHERE id = ?", id);
        if (rows.isEmpty()) return Optional.empty();

        Map<String, Object> row = rows.getFirst();
        return Optional.of(buildProject(row));
    }

    public List<Project> findByUserLogin(String login) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT DISTINCT p.id, p.name, p.manager_login FROM projects p
            LEFT JOIN project_members pm ON pm.project_id = p.id
            WHERE p.manager_login = ? OR pm.user_login = ?
            """, login, login);

        return rows.stream().map(this::buildProject).toList();
    }

    public List<Project> findAll() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, name, manager_login FROM projects");
        return rows.stream().map(this::buildProject).toList();
    }

    public boolean existsById(UUID id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM projects WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    public ProjectRole getRoleInProject(UUID projectId, String userLogin) {
        List<Map<String, Object>> projectRows = jdbc.queryForList(
                "SELECT manager_login FROM projects WHERE id = ?", projectId);
        if (projectRows.isEmpty()) return null;

        String manager = (String) projectRows.getFirst().get("manager_login");
        if (manager.equals(userLogin)) return ProjectRole.MANAGER;

        List<String> roles = jdbc.queryForList(
                "SELECT role FROM project_members WHERE project_id = ? AND user_login = ?",
                String.class, projectId, userLogin);
        if (roles.isEmpty()) return null;
        return ProjectRole.valueOf(roles.getFirst());
    }

    private Project buildProject(Map<String, Object> row) {
        UUID id = (UUID) row.get("id");
        String name = (String) row.get("name");
        String manager = (String) row.get("manager_login");

        String teamLeader = null;
        Set<String> developers = new LinkedHashSet<>();
        Set<String> testers = new LinkedHashSet<>();

        List<Map<String, Object>> members = jdbc.queryForList(
                "SELECT user_login, role FROM project_members WHERE project_id = ?", id);
        for (Map<String, Object> m : members) {
            String login = (String) m.get("user_login");
            String role = (String) m.get("role");
            switch (role) {
                case "TEAM_LEADER" -> teamLeader = login;
                case "DEVELOPER" -> developers.add(login);
                case "TESTER" -> testers.add(login);
            }
        }

        return new Project(id, name, manager, teamLeader, developers, testers);
    }
}



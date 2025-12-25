package org.lab.repository;

import org.lab.domain.Project;
import org.lab.domain.ProjectRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        jdbc.update("DELETE FROM project_members WHERE project_id = ?", project.id());

        Optional.ofNullable(project.teamLeaderLogin())
                .ifPresent(lead -> jdbc.update(
                        "INSERT INTO project_members (project_id, user_login, role) VALUES (?, ?, ?)",
                        project.id(), lead, ProjectRole.TEAM_LEADER.name()));

        Stream.concat(
                project.developerLogins().stream()
                        .map(dev -> new Object[]{project.id(), dev, ProjectRole.DEVELOPER.name()}),
                project.testerLogins().stream()
                        .map(tester -> new Object[]{project.id(), tester, ProjectRole.TESTER.name()})
        ).forEach(params -> jdbc.update(
                "INSERT INTO project_members (project_id, user_login, role) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                params));
    }

    public Optional<Project> findById(UUID id) {
        return jdbc.queryForList("SELECT id, name, manager_login FROM projects WHERE id = ?", id)
                .stream()
                .findFirst()
                .map(this::buildProject);
    }

    public List<Project> findByUserLogin(String login) {
        return jdbc.queryForList("""
            SELECT DISTINCT p.id, p.name, p.manager_login FROM projects p
            LEFT JOIN project_members pm ON pm.project_id = p.id
            WHERE p.manager_login = ? OR pm.user_login = ?
            """, login, login)
                .stream()
                .map(this::buildProject)
                .toList();
    }

    public List<Project> findAll() {
        return jdbc.queryForList("SELECT id, name, manager_login FROM projects")
                .stream()
                .map(this::buildProject)
                .toList();
    }

    public boolean existsById(UUID id) {
        return Optional.ofNullable(
                jdbc.queryForObject("SELECT COUNT(*) FROM projects WHERE id = ?", Integer.class, id)
        ).map(count -> count > 0).orElse(false);
    }

    public ProjectRole getRoleInProject(UUID projectId, String userLogin) {
        return jdbc.queryForList("SELECT manager_login FROM projects WHERE id = ?", projectId)
                .stream()
                .findFirst()
                .map(row -> (String) row.get("manager_login"))
                .filter(manager -> manager.equals(userLogin))
                .map(_ -> ProjectRole.MANAGER)
                .orElseGet(() -> jdbc.queryForList(
                                "SELECT role FROM project_members WHERE project_id = ? AND user_login = ?",
                                String.class, projectId, userLogin)
                        .stream()
                        .findFirst()
                        .map(ProjectRole::valueOf)
                        .orElse(null));
    }

    private Project buildProject(Map<String, Object> row) {
        UUID id = (UUID) row.get("id");
        String name = (String) row.get("name");
        String manager = (String) row.get("manager_login");

        Map<ProjectRole, List<String>> membersByRole = jdbc.queryForList(
                        "SELECT user_login, role FROM project_members WHERE project_id = ?", id)
                .stream()
                .collect(Collectors.groupingBy(
                        memberRow -> ProjectRole.valueOf((String) memberRow.get("role")),
                        Collectors.mapping(
                                memberRow -> (String) memberRow.get("user_login"),
                                Collectors.toList())));

        String teamLeader = membersByRole.getOrDefault(ProjectRole.TEAM_LEADER, List.of())
                .stream()
                .findFirst()
                .orElse(null);

        Set<String> developers = new LinkedHashSet<>(
                membersByRole.getOrDefault(ProjectRole.DEVELOPER, List.of()));
        Set<String> testers = new LinkedHashSet<>(
                membersByRole.getOrDefault(ProjectRole.TESTER, List.of()));

        return new Project(id, name, manager, teamLeader, developers, testers);
    }
}

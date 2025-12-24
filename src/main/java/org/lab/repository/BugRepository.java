package org.lab.repository;

import org.lab.domain.BugReport;
import org.lab.domain.BugStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BugRepository {
    private final JdbcTemplate jdbc;

    private static final RowMapper<BugReport> ROW_MAPPER = (rs, _) -> new BugReport(
            rs.getObject("id", UUID.class),
            rs.getObject("project_id", UUID.class),
            rs.getString("title"),
            rs.getString("reporter_login"),
            rs.getString("assignee_login"),
            BugStatus.valueOf(rs.getString("status"))
    );

    public BugRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(BugReport b) {
        jdbc.update("""
            INSERT INTO bug_reports (id, project_id, title, reporter_login, assignee_login, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                assignee_login = EXCLUDED.assignee_login,
                status = EXCLUDED.status
            """, b.id(), b.projectId(), b.title(), b.reporterLogin(), b.assigneeDeveloperLogin(), b.status().name());
    }

    public Optional<BugReport> findById(UUID id) {
        List<BugReport> result = jdbc.query(
                "SELECT id, project_id, title, reporter_login, assignee_login, status FROM bug_reports WHERE id = ?",
                ROW_MAPPER, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    public List<BugReport> findByProjectId(UUID projectId) {
        return jdbc.query(
                "SELECT id, project_id, title, reporter_login, assignee_login, status FROM bug_reports WHERE project_id = ? ORDER BY title",
                ROW_MAPPER, projectId);
    }

    public List<BugReport> findByAssignee(String userLogin) {
        return jdbc.query(
                "SELECT id, project_id, title, reporter_login, assignee_login, status FROM bug_reports WHERE assignee_login = ? ORDER BY title",
                ROW_MAPPER, userLogin);
    }

    public List<BugReport> findNeedsTesting(UUID projectId) {
        return jdbc.query(
                "SELECT id, project_id, title, reporter_login, assignee_login, status FROM bug_reports WHERE project_id = ? AND status = 'FIXED' ORDER BY title",
                ROW_MAPPER, projectId);
    }
}



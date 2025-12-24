package org.lab.repository;

import org.lab.domain.Milestone;
import org.lab.domain.MilestoneStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MilestoneRepository {
    private final JdbcTemplate jdbc;

    private static final RowMapper<Milestone> ROW_MAPPER = (rs, _) -> new Milestone(
            rs.getObject("id", UUID.class),
            rs.getObject("project_id", UUID.class),
            rs.getString("name"),
            rs.getDate("start_date").toLocalDate(),
            rs.getDate("end_date").toLocalDate(),
            MilestoneStatus.valueOf(rs.getString("status"))
    );

    public MilestoneRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(Milestone m) {
        jdbc.update("""
            INSERT INTO milestones (id, project_id, name, start_date, end_date, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                start_date = EXCLUDED.start_date,
                end_date = EXCLUDED.end_date,
                status = EXCLUDED.status
            """,
                m.id(), m.projectId(), m.name(),
                Date.valueOf(m.startDate()), Date.valueOf(m.endDate()),
                m.status().name());
    }

    public Optional<Milestone> findById(UUID id) {
        List<Milestone> result = jdbc.query(
                "SELECT id, project_id, name, start_date, end_date, status FROM milestones WHERE id = ?",
                ROW_MAPPER, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    public List<Milestone> findByProjectId(UUID projectId) {
        return jdbc.query(
                "SELECT id, project_id, name, start_date, end_date, status FROM milestones WHERE project_id = ? ORDER BY start_date",
                ROW_MAPPER, projectId);
    }

    public boolean hasCurrentMilestone(UUID projectId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM milestones WHERE project_id = ? AND status IN ('OPEN', 'ACTIVE')",
                Integer.class, projectId);
        return count != null && count > 0;
    }

    public boolean hasActiveMilestone(UUID projectId, UUID excludeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM milestones WHERE project_id = ? AND status = 'ACTIVE' AND id != ?",
                Integer.class, projectId, excludeId);
        return count != null && count > 0;
    }
}



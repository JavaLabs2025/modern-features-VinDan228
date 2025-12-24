package org.lab.repository;

import org.lab.domain.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    private static final RowMapper<User> ROW_MAPPER = (rs, _) ->
            new User(rs.getString("login"), rs.getString("name"));

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(User user) {
        jdbc.update("""
            INSERT INTO users (login, name) VALUES (?, ?)
            ON CONFLICT (login) DO UPDATE SET name = EXCLUDED.name
            """, user.login(), user.name());
    }

    public Optional<User> findByLogin(String login) {
        List<User> result = jdbc.query("SELECT login, name FROM users WHERE login = ?", ROW_MAPPER, login);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    public boolean existsByLogin(String login) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE login = ?", Integer.class, login);
        return count != null && count > 0;
    }

    public List<User> findAll() {
        return jdbc.query("SELECT login, name FROM users", ROW_MAPPER);
    }
}



package com.dongyao.myblog.blog.auth;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        String sql = "select id, username, password_hash, role from user_account where username = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserAccount(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role")
        ), username).stream().findFirst();
    }

    @Override
    public UserAccount save(String username, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into user_account(username, password_hash, role) values (?, ?, 'USER')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            return ps;
        }, keyHolder);
        return new UserAccount(keyHolder.getKey().longValue(), username, passwordHash, "USER");
    }
}

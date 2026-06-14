package com.dongyao.myblog.blog.ai;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAiTaskRepository implements AiTaskRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAiTaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiTask createSummaryTask(Long articleId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into ai_task(task_type, article_id, status) values ('SUMMARY', ?, 'PENDING')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, articleId);
            return ps;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue()).orElseThrow();
    }

    public Optional<AiTask> findById(Long id) {
        String sql = "select id, article_id, status, result, error_message, created_at, updated_at from ai_task where id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AiTask(
                rs.getLong("id"),
                rs.getLong("article_id"),
                AiTaskStatus.valueOf(rs.getString("status")),
                rs.getString("result"),
                rs.getString("error_message"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        ), id).stream().findFirst();
    }

    public AiTask markProcessing(Long id) {
        jdbcTemplate.update("update ai_task set status = 'PROCESSING', updated_at = current_timestamp where id = ?", id);
        return findById(id).orElseThrow();
    }

    public AiTask markSuccess(Long id, String result) {
        jdbcTemplate.update("update ai_task set status = 'SUCCESS', result = ?, error_message = null, updated_at = current_timestamp where id = ?", result, id);
        return findById(id).orElseThrow();
    }

    public AiTask markFailed(Long id, String errorMessage) {
        jdbcTemplate.update("update ai_task set status = 'FAILED', error_message = ?, updated_at = current_timestamp where id = ?", errorMessage, id);
        return findById(id).orElseThrow();
    }
}

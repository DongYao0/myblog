package com.dongyao.myblog.blog.article;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcArticleRepository implements ArticleRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcArticleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Article save(String title, String content, Long authorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into article(title, content, author_id, status) values (?, ?, ?, 'PUBLISHED')",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setLong(3, authorId);
            return ps;
        }, keyHolder);
        return findById(keyHolder.getKey().longValue()).orElseThrow();
    }

    @Override
    public Optional<Article> findById(Long id) {
        String sql = "select id, title, content, author_id, status, created_at, updated_at from article where id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapArticle(rs), id).stream().findFirst();
    }

    @Override
    public List<Article> findAll() {
        String sql = "select id, title, content, author_id, status, created_at, updated_at from article order by id desc";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapArticle(rs));
    }

    private Article mapArticle(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Article(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getLong("author_id"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }

    @Override
    public Article update(Long id, String title, String content) {
        jdbcTemplate.update("update article set title = ?, content = ?, updated_at = current_timestamp where id = ?",
                title, content, id);
        return findById(id).orElseThrow(() -> new IllegalArgumentException("article not found"));
    }

    @Override
    public Optional<Article> delete(Long id) {
        Optional<Article> existing = findById(id);
        existing.ifPresent(article -> jdbcTemplate.update("delete from article where id = ?", id));
        return existing;
    }
}

package com.dongyao.myblog.blog.article;

import java.time.Instant;

public record Article(Long id, String title, String content, Long authorId, String status, Instant createdAt, Instant updatedAt) {
    public static Article create(Long id, String title, String content, Long authorId) {
        Instant now = Instant.now();
        return new Article(id, title, content, authorId, "PUBLISHED", now, now);
    }

    public Article withContent(String title, String content) {
        return new Article(id, title, content, authorId, status, createdAt, Instant.now());
    }
}

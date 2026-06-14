package com.dongyao.myblog.blog.ai;

import java.time.Instant;

public record AiTask(Long id, Long articleId, AiTaskStatus status, String result, String errorMessage, Instant createdAt, Instant updatedAt) {
    public static AiTask pending(Long id, Long articleId) {
        Instant now = Instant.now();
        return new AiTask(id, articleId, AiTaskStatus.PENDING, null, null, now, now);
    }

    public AiTask withStatus(AiTaskStatus status, String result, String errorMessage) {
        return new AiTask(id, articleId, status, result, errorMessage, createdAt, Instant.now());
    }
}

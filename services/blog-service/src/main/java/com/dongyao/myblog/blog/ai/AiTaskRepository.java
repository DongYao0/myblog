package com.dongyao.myblog.blog.ai;

import java.util.Optional;

public interface AiTaskRepository {
    AiTask createSummaryTask(Long articleId);

    Optional<AiTask> findById(Long id);

    AiTask markProcessing(Long id);

    AiTask markSuccess(Long id, String result);

    AiTask markFailed(Long id, String errorMessage);
}

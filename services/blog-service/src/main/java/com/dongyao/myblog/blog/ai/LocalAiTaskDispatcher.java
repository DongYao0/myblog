package com.dongyao.myblog.blog.ai;

import org.springframework.stereotype.Component;

@Component
public class LocalAiTaskDispatcher implements AiTaskDispatcher {
    private final AiTaskRepository tasks;

    public LocalAiTaskDispatcher(AiTaskRepository tasks) {
        this.tasks = tasks;
    }

    public void dispatch(AiTask task) {
        tasks.markProcessing(task.id());
        tasks.markSuccess(task.id(), "Local AI summary task accepted for article " + task.articleId());
    }
}

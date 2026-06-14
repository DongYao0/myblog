package com.dongyao.myblog.blog.ai;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiTaskService {
    private final AiTaskRepository tasks;
    private final AiTaskDispatcher dispatcher;

    public AiTaskService(AiTaskRepository tasks, AiTaskDispatcher dispatcher) {
        this.tasks = tasks;
        this.dispatcher = dispatcher;
    }

    public AiTask submitSummary(SummaryTaskRequest request) {
        if (request.articleId() == null) {
            throw new IllegalArgumentException("articleId is required");
        }
        AiTask task = tasks.createSummaryTask(request.articleId());
        try {
            dispatcher.dispatch(task);
        } catch (Exception ex) {
            tasks.markFailed(task.id(), "failed to dispatch ai task");
        }
        return task;
    }

    public Optional<AiTask> findById(Long id) {
        return tasks.findById(id);
    }

    public AiTask complete(AiTaskResultRequest request) {
        if (request.errorMessage() != null && !request.errorMessage().isBlank()) {
            return tasks.markFailed(request.taskId(), request.errorMessage());
        }
        return tasks.markSuccess(request.taskId(), request.result());
    }
}

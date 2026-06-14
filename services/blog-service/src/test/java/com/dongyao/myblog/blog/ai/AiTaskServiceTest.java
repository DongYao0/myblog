package com.dongyao.myblog.blog.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiTaskServiceTest {
    @Test
    void submitsAndCompletesSummaryTask() {
        FakeAiTaskRepository tasks = new FakeAiTaskRepository();
        AiTaskService service = new AiTaskService(tasks, task -> tasks.markProcessing(task.id()));

        AiTask submitted = service.submitSummary(new SummaryTaskRequest(7L));
        AiTask completed = service.complete(new AiTaskResultRequest(submitted.id(), "summary", null));

        assertThat(submitted.status()).isEqualTo(AiTaskStatus.PENDING);
        assertThat(tasks.findById(submitted.id()).orElseThrow().status()).isEqualTo(AiTaskStatus.SUCCESS);
        assertThat(completed.result()).isEqualTo("summary");
    }

    @Test
    void marksTaskFailedWhenResultContainsError() {
        FakeAiTaskRepository tasks = new FakeAiTaskRepository();
        AiTaskService service = new AiTaskService(tasks, task -> tasks.markProcessing(task.id()));
        AiTask submitted = service.submitSummary(new SummaryTaskRequest(8L));

        AiTask failed = service.complete(new AiTaskResultRequest(submitted.id(), null, "llm timeout"));

        assertThat(failed.status()).isEqualTo(AiTaskStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("llm timeout");
    }

    private static final class FakeAiTaskRepository implements AiTaskRepository {
        private long sequence = 1;
        private final Map<Long, AiTask> rows = new LinkedHashMap<>();

        public AiTask createSummaryTask(Long articleId) {
            AiTask task = AiTask.pending(sequence++, articleId);
            rows.put(task.id(), task);
            return task;
        }

        public Optional<AiTask> findById(Long id) {
            return Optional.ofNullable(rows.get(id));
        }

        public AiTask markProcessing(Long id) {
            return update(id, rows.get(id).withStatus(AiTaskStatus.PROCESSING, null, null));
        }

        public AiTask markSuccess(Long id, String result) {
            return update(id, rows.get(id).withStatus(AiTaskStatus.SUCCESS, result, null));
        }

        public AiTask markFailed(Long id, String errorMessage) {
            return update(id, rows.get(id).withStatus(AiTaskStatus.FAILED, null, errorMessage));
        }

        private AiTask update(Long id, AiTask task) {
            rows.put(id, task);
            return task;
        }
    }
}

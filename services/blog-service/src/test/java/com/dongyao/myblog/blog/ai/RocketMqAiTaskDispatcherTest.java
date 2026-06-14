package com.dongyao.myblog.blog.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RocketMqAiTaskDispatcherTest {
    @Test
    void fallsBackToLocalDispatcherWhenRocketMqSendFails() {
        RecordingRepository tasks = new RecordingRepository();
        LocalAiTaskDispatcher local = new LocalAiTaskDispatcher(tasks);
        RocketMqAiTaskDispatcher dispatcher = new RocketMqAiTaskDispatcher(task -> {
            throw new IllegalStateException("rocketmq unavailable");
        }, local);

        dispatcher.dispatch(AiTask.pending(3L, 9L));

        assertThat(tasks.processedTaskId).isEqualTo(3L);
        assertThat(tasks.successResult).contains("article 9");
    }

    private static final class RecordingRepository implements AiTaskRepository {
        private Long processedTaskId;
        private String successResult;

        public AiTask createSummaryTask(Long articleId) {
            return AiTask.pending(3L, articleId);
        }

        public java.util.Optional<AiTask> findById(Long id) {
            return java.util.Optional.of(AiTask.pending(id, 9L));
        }

        public AiTask markProcessing(Long id) {
            processedTaskId = id;
            return AiTask.pending(id, 9L).withStatus(AiTaskStatus.PROCESSING, null, null);
        }

        public AiTask markSuccess(Long id, String result) {
            successResult = result;
            return AiTask.pending(id, 9L).withStatus(AiTaskStatus.SUCCESS, result, null);
        }

        public AiTask markFailed(Long id, String errorMessage) {
            return AiTask.pending(id, 9L).withStatus(AiTaskStatus.FAILED, null, errorMessage);
        }
    }
}

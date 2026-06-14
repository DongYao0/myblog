package com.dongyao.myblog.blog.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RocketMqAiTaskConsumerTest {
    @Test
    void consumesAiSummaryMessageAndMarksTaskSuccess() throws Exception {
        RecordingRepository tasks = new RecordingRepository();
        RocketMqAiTaskConsumer consumer = new RocketMqAiTaskConsumer(tasks, "localhost:9876", "test-group", "ai-task-topic");

        consumer.handleMessage("{\"taskId\":5,\"articleId\":11}");

        assertThat(tasks.processedTaskId).isEqualTo(5L);
        assertThat(tasks.successResult).isEqualTo("RocketMQ AI summary task accepted for article 11");
    }

    private static final class RecordingRepository implements AiTaskRepository {
        private Long processedTaskId;
        private String successResult;

        public AiTask createSummaryTask(Long articleId) {
            return AiTask.pending(5L, articleId);
        }

        public Optional<AiTask> findById(Long id) {
            return Optional.of(AiTask.pending(id, 11L));
        }

        public AiTask markProcessing(Long id) {
            processedTaskId = id;
            return AiTask.pending(id, 11L).withStatus(AiTaskStatus.PROCESSING, null, null);
        }

        public AiTask markSuccess(Long id, String result) {
            successResult = result;
            return AiTask.pending(id, 11L).withStatus(AiTaskStatus.SUCCESS, result, null);
        }

        public AiTask markFailed(Long id, String errorMessage) {
            return AiTask.pending(id, 11L).withStatus(AiTaskStatus.FAILED, null, errorMessage);
        }
    }
}

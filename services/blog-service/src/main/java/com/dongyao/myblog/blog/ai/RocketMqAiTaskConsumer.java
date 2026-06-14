package com.dongyao.myblog.blog.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocketMqAiTaskConsumer {
    private static final Logger log = LoggerFactory.getLogger(RocketMqAiTaskConsumer.class);

    private final AiTaskRepository tasks;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String namesrvAddr;
    private final String consumerGroup;
    private final String topic;
    private DefaultMQPushConsumer consumer;

    public RocketMqAiTaskConsumer(
            AiTaskRepository tasks,
            @Value("${myblog.rocketmq.namesrv-addr:localhost:9876}") String namesrvAddr,
            @Value("${myblog.rocketmq.consumer-group:blog-ai-task-consumer}") String consumerGroup,
            @Value("${myblog.rocketmq.topic:ai-task-topic}") String topic) {
        this.tasks = tasks;
        this.namesrvAddr = namesrvAddr;
        this.consumerGroup = consumerGroup;
        this.topic = topic;
    }

    @PostConstruct
    public void start() {
        try {
            DefaultMQPushConsumer created = new DefaultMQPushConsumer(consumerGroup);
            created.setNamesrvAddr(namesrvAddr);
            created.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
            created.subscribe(topic, "AI_SUMMARY_REQUESTED");
            created.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {
                try {
                    for (var message : messages) {
                        handleMessage(new String(message.getBody(), StandardCharsets.UTF_8));
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                } catch (Exception ex) {
                    log.warn("failed to consume ai task message", ex);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            });
            created.start();
            consumer = created;
        } catch (Exception ex) {
            log.warn("rocketmq ai task consumer disabled", ex);
        }
    }

    void handleMessage(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        long taskId = root.path("taskId").asLong();
        long articleId = root.path("articleId").asLong();
        AiTask current = tasks.findById(taskId).orElseThrow();
        if (current.status() == AiTaskStatus.SUCCESS) {
            return;
        }
        tasks.markProcessing(taskId);
        tasks.markSuccess(taskId, "RocketMQ AI summary task accepted for article " + articleId);
    }

    @PreDestroy
    public void shutdown() {
        if (consumer != null) {
            consumer.shutdown();
            consumer = null;
        }
    }
}

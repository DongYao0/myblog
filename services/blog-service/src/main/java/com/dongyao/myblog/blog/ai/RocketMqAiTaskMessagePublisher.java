package com.dongyao.myblog.blog.ai;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RocketMqAiTaskMessagePublisher implements AiTaskMessagePublisher {
    private final String namesrvAddr;
    private final String producerGroup;
    private final String topic;
    private DefaultMQProducer producer;

    public RocketMqAiTaskMessagePublisher(
            @Value("${myblog.rocketmq.namesrv-addr:localhost:9876}") String namesrvAddr,
            @Value("${myblog.rocketmq.producer-group:blog-ai-task-producer}") String producerGroup,
            @Value("${myblog.rocketmq.topic:ai-task-topic}") String topic) {
        this.namesrvAddr = namesrvAddr;
        this.producerGroup = producerGroup;
        this.topic = topic;
    }

    public synchronized void publish(AiTask task) {
        try {
            ensureStarted();
            String body = "{\"taskId\":" + task.id() + ",\"articleId\":" + task.articleId() + "}";
            producer.send(new Message(topic, "AI_SUMMARY_REQUESTED", body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("failed to publish ai task to rocketmq", ex);
        }
    }

    private void ensureStarted() throws Exception {
        if (producer != null) {
            return;
        }
        DefaultMQProducer created = new DefaultMQProducer(producerGroup);
        created.setNamesrvAddr(namesrvAddr);
        created.start();
        producer = created;
    }

    @PreDestroy
    public synchronized void shutdown() {
        if (producer != null) {
            producer.shutdown();
            producer = null;
        }
    }
}

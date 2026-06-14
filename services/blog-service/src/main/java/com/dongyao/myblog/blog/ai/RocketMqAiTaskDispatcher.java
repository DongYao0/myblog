package com.dongyao.myblog.blog.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class RocketMqAiTaskDispatcher implements AiTaskDispatcher {
    private final AiTaskMessagePublisher publisher;
    private final LocalAiTaskDispatcher fallback;

    public RocketMqAiTaskDispatcher(AiTaskMessagePublisher publisher, LocalAiTaskDispatcher fallback) {
        this.publisher = publisher;
        this.fallback = fallback;
    }

    public void dispatch(AiTask task) {
        try {
            publisher.publish(task);
        } catch (Exception ex) {
            fallback.dispatch(task);
        }
    }
}

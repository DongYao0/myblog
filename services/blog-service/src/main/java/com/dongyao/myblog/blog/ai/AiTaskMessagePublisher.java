package com.dongyao.myblog.blog.ai;

@FunctionalInterface
public interface AiTaskMessagePublisher {
    void publish(AiTask task);
}

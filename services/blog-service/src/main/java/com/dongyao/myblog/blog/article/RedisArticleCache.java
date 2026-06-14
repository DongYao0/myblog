package com.dongyao.myblog.blog.article;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisArticleCache implements ArticleCache {
    private static final Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisArticleCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public Optional<Article> get(Long id) {
        try {
            String value = redisTemplate.opsForValue().get(key(id));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, Article.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public void put(Article article) {
        try {
            redisTemplate.opsForValue().set(key(article.id()), objectMapper.writeValueAsString(article), TTL);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void evict(Long id) {
        try {
            redisTemplate.delete(key(id));
        } catch (Exception ignored) {
        }
    }

    private static String key(Long id) {
        return "article:detail:" + id;
    }
}

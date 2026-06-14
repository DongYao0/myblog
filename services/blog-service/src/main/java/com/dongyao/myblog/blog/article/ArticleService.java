package com.dongyao.myblog.blog.article;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {
    private final ArticleRepository articles;
    private final ArticleCache cache;

    public ArticleService(ArticleRepository articles) {
        this(articles, new NoopArticleCache());
    }

    public ArticleService(ArticleRepository articles, ArticleCache cache) {
        this.articles = articles;
        this.cache = cache;
    }

    public Article create(ArticleCreateRequest request) {
        validate(request.title(), request.content());
        Long authorId = request.authorId() == null ? 0L : request.authorId();
        return articles.save(request.title(), request.content(), authorId);
    }

    public Optional<Article> get(Long id) {
        Optional<Article> cached = cache.get(id);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<Article> loaded = articles.findById(id);
        loaded.ifPresent(cache::put);
        return loaded;
    }

    public Article update(Long id, ArticleUpdateRequest request) {
        validate(request.title(), request.content());
        Article updated = articles.update(id, request.title(), request.content());
        cache.evict(id);
        return updated;
    }

    public Optional<Article> delete(Long id) {
        Optional<Article> deleted = articles.delete(id);
        cache.evict(id);
        return deleted;
    }

    private static void validate(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
    }

    private static final class NoopArticleCache implements ArticleCache {
        public Optional<Article> get(Long id) {
            return Optional.empty();
        }

        public void put(Article article) {
        }

        public void evict(Long id) {
        }
    }
}

package com.dongyao.myblog.blog.article;

import com.dongyao.myblog.blog.search.ArticleSearchService;
import com.dongyao.myblog.blog.search.SearchDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {
    private final ArticleRepository articles;
    private final ArticleCache cache;
    private final ArticleSearchService searchService;

    public ArticleService(ArticleRepository articles) {
        this(articles, new NoopArticleCache(), null);
    }

    public ArticleService(ArticleRepository articles, ArticleCache cache) {
        this(articles, cache, null);
    }

    @Autowired
    public ArticleService(ArticleRepository articles, ArticleCache cache, ArticleSearchService searchService) {
        this.articles = articles;
        this.cache = cache;
        this.searchService = searchService;
    }

    public Article create(ArticleCreateRequest request) {
        validate(request.title(), request.content());
        Long authorId = request.authorId() == null ? 0L : request.authorId();
        Article created = articles.save(request.title(), request.content(), authorId);
        index(created);
        return created;
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

    public List<Article> list() {
        return articles.findAll();
    }

    public Article update(Long id, ArticleUpdateRequest request) {
        validate(request.title(), request.content());
        Article updated = articles.update(id, request.title(), request.content());
        cache.evict(id);
        index(updated);
        return updated;
    }

    public Optional<Article> delete(Long id) {
        Optional<Article> deleted = articles.delete(id);
        cache.evict(id);
        if (searchService != null) {
            searchService.delete(id);
        }
        return deleted;
    }

    private void index(Article article) {
        if (searchService != null) {
            searchService.index(SearchDocument.from(article));
        }
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

package com.dongyao.myblog.blog.article;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {
    private final ArticleRepository articles;

    public ArticleService(ArticleRepository articles) {
        this.articles = articles;
    }

    public Article create(ArticleCreateRequest request) {
        validate(request.title(), request.content());
        Long authorId = request.authorId() == null ? 0L : request.authorId();
        return articles.save(request.title(), request.content(), authorId);
    }

    public Optional<Article> get(Long id) {
        return articles.findById(id);
    }

    public Article update(Long id, ArticleUpdateRequest request) {
        validate(request.title(), request.content());
        return articles.update(id, request.title(), request.content());
    }

    public Optional<Article> delete(Long id) {
        return articles.delete(id);
    }

    private static void validate(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
    }
}

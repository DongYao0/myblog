package com.dongyao.myblog.blog.search;

import com.dongyao.myblog.blog.article.Article;

public record SearchDocument(Long id, String title, String content, Long authorId, String status) {
    public static SearchDocument from(Article article) {
        return new SearchDocument(article.id(), article.title(), article.content(), article.authorId(), article.status());
    }
}

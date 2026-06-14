package com.dongyao.myblog.blog.article;

import java.util.Optional;

public interface ArticleRepository {
    Article save(String title, String content, Long authorId);

    Optional<Article> findById(Long id);

    Article update(Long id, String title, String content);

    Optional<Article> delete(Long id);
}

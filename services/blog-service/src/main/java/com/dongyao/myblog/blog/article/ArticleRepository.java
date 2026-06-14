package com.dongyao.myblog.blog.article;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository {
    Article save(String title, String content, Long authorId);

    Optional<Article> findById(Long id);

    List<Article> findAll();

    Article update(Long id, String title, String content);

    Optional<Article> delete(Long id);
}

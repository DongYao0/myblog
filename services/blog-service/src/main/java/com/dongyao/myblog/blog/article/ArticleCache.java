package com.dongyao.myblog.blog.article;

import java.util.Optional;

public interface ArticleCache {
    Optional<Article> get(Long id);

    void put(Article article);

    void evict(Long id);
}

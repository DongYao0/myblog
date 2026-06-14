package com.dongyao.myblog.blog.search;

import java.util.List;

public interface ArticleSearchClient {
    void index(SearchDocument document);

    void delete(Long articleId);

    List<SearchResult> search(String keyword);
}

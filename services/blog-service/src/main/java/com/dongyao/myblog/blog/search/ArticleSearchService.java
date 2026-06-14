package com.dongyao.myblog.blog.search;

import org.springframework.stereotype.Service;

@Service
public class ArticleSearchService {
    private final ArticleSearchClient searchClient;

    public ArticleSearchService(ArticleSearchClient searchClient) {
        this.searchClient = searchClient;
    }

    public void index(SearchDocument document) {
        try {
            searchClient.index(document);
        } catch (Exception ignored) {
        }
    }

    public void delete(Long articleId) {
        try {
            searchClient.delete(articleId);
        } catch (Exception ignored) {
        }
    }

    public SearchResponse search(String keyword) {
        try {
            return SearchResponse.ok(searchClient.search(keyword));
        } catch (Exception ex) {
            return SearchResponse.degraded("search temporarily unavailable");
        }
    }
}

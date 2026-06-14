package com.dongyao.myblog.blog.search;

import java.util.List;

public record SearchResponse(boolean degraded, String message, List<SearchResult> results) {
    public static SearchResponse ok(List<SearchResult> results) {
        return new SearchResponse(false, "ok", results);
    }

    public static SearchResponse degraded(String message) {
        return new SearchResponse(true, message, List.of());
    }
}

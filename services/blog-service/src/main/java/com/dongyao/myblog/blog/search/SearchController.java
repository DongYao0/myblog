package com.dongyao.myblog.blog.search;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog/search")
public class SearchController {
    private final ArticleSearchService searchService;

    public SearchController(ArticleSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(@RequestParam("keyword") String keyword) {
        return searchService.search(keyword);
    }
}

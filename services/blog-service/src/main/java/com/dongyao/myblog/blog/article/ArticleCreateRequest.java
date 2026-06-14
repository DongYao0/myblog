package com.dongyao.myblog.blog.article;

public record ArticleCreateRequest(String title, String content, Long authorId) {
}

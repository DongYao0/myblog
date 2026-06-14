package com.dongyao.myblog.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArticleCacheServiceTest {
    @Test
    void readsFromCacheAfterFirstRepositoryLoadAndInvalidatesOnUpdate() {
        CountingArticleRepository repository = new CountingArticleRepository();
        FakeArticleCache cache = new FakeArticleCache();
        ArticleService service = new ArticleService(repository, cache);
        Article article = service.create(new ArticleCreateRequest("title", "content", 1L));

        service.get(article.id());
        service.get(article.id());
        service.update(article.id(), new ArticleUpdateRequest("new title", "new content"));

        assertThat(repository.findCount).isEqualTo(1);
        assertThat(cache.invalidatedId).isEqualTo(article.id());
    }

    private static final class FakeArticleCache implements ArticleCache {
        private final Map<Long, Article> rows = new HashMap<>();
        private Long invalidatedId;

        public Optional<Article> get(Long id) {
            return Optional.ofNullable(rows.get(id));
        }

        public void put(Article article) {
            rows.put(article.id(), article);
        }

        public void evict(Long id) {
            invalidatedId = id;
            rows.remove(id);
        }
    }

    private static final class CountingArticleRepository implements ArticleRepository {
        private Article article;
        private int findCount;

        public Article save(String title, String content, Long authorId) {
            article = Article.create(1L, title, content, authorId);
            return article;
        }

        public Optional<Article> findById(Long id) {
            findCount++;
            return Optional.ofNullable(article);
        }

        public java.util.List<Article> findAll() {
            return article == null ? java.util.List.of() : java.util.List.of(article);
        }

        public Article update(Long id, String title, String content) {
            article = article.withContent(title, content);
            return article;
        }

        public Optional<Article> delete(Long id) {
            Article deleted = article;
            article = null;
            return Optional.ofNullable(deleted);
        }
    }
}

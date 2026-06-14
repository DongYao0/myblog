package com.dongyao.myblog.blog.article;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArticleServiceTest {
    @Test
    void createsReadsUpdatesAndDeletesArticle() {
        FakeArticleRepository articles = new FakeArticleRepository();
        ArticleService service = new ArticleService(articles);

        Article created = service.create(new ArticleCreateRequest("title", "content", 7L));
        Article updated = service.update(created.id(), new ArticleUpdateRequest("new title", "new content"));

        assertThat(created.id()).isPositive();
        assertThat(service.get(created.id()).orElseThrow().title()).isEqualTo("new title");
        assertThat(updated.content()).isEqualTo("new content");
        Optional<Article> deleted = service.delete(created.id());
        assertThat(deleted).isPresent();
        assertThat(service.get(created.id())).isEmpty();
    }

    private static final class FakeArticleRepository implements ArticleRepository {
        private long sequence = 1;
        private final Map<Long, Article> rows = new LinkedHashMap<>();

        @Override
        public Article save(String title, String content, Long authorId) {
            Article article = Article.create(sequence++, title, content, authorId);
            rows.put(article.id(), article);
            return article;
        }

        @Override
        public Optional<Article> findById(Long id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public Article update(Long id, String title, String content) {
            Article current = rows.get(id);
            Article updated = current.withContent(title, content);
            rows.put(id, updated);
            return updated;
        }

        @Override
        public Optional<Article> delete(Long id) {
            return Optional.ofNullable(rows.remove(id));
        }
    }
}

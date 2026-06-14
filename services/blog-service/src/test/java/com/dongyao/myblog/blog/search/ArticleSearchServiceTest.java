package com.dongyao.myblog.blog.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongyao.myblog.blog.article.Article;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArticleSearchServiceTest {
    @Test
    void mapsArticleToSearchDocument() {
        Article article = Article.create(9L, "search title", "search content", 3L);
        SearchDocument document = SearchDocument.from(article);

        assertThat(document.id()).isEqualTo(9L);
        assertThat(document.title()).isEqualTo("search title");
        assertThat(document.content()).isEqualTo("search content");
    }

    @Test
    void returnsDegradedResultWhenSearchClientFails() {
        ArticleSearchService service = new ArticleSearchService(new FailingArticleSearchClient());

        SearchResponse response = service.search("spring");

        assertThat(response.degraded()).isTrue();
        assertThat(response.results()).isEmpty();
        assertThat(response.message()).contains("search temporarily unavailable");
    }

    private static final class FailingArticleSearchClient implements ArticleSearchClient {
        public void index(SearchDocument document) {
            throw new IllegalStateException("boom");
        }

        public void delete(Long articleId) {
            throw new IllegalStateException("boom");
        }

        public List<SearchResult> search(String keyword) {
            throw new IllegalStateException("boom");
        }
    }
}

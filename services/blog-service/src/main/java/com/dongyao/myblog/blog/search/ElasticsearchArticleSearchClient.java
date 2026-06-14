package com.dongyao.myblog.blog.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchArticleSearchClient implements ArticleSearchClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseUrl;

    public ElasticsearchArticleSearchClient(@Value("${myblog.elasticsearch.base-url:http://localhost:9200}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void index(SearchDocument document) {
        try {
            String body = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/blog_article/_doc/" + document.id()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to index article", ex);
        }
    }

    public void delete(Long articleId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/blog_article/_doc/" + articleId))
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ex) {
            throw new IllegalStateException("failed to delete article index", ex);
        }
    }

    public List<SearchResult> search(String keyword) {
        try {
            String body = "{\"query\":{\"multi_match\":{\"query\":\"" + escape(keyword) + "\",\"fields\":[\"title\",\"content\"]}}}";
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/blog_article/_search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            String response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return parseResults(response);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to search articles", ex);
        }
    }

    private List<SearchResult> parseResults(String response) throws Exception {
        JsonNode hits = objectMapper.readTree(response).path("hits").path("hits");
        List<SearchResult> results = new ArrayList<>();
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            results.add(new SearchResult(source.path("id").asLong(), source.path("title").asText(), source.path("content").asText("")));
        }
        return results;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

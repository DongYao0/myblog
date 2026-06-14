package com.dongyao.myblog.gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {
    @Test
    void allowsPublicAuthRouteWithoutToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(path -> false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/blog/auth/login").build());

        filter.filter(exchange, current -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void allowsPublicBlogReadRoutesWithoutToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(path -> false);
        MockServerWebExchange listExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/blog/articles").build());
        MockServerWebExchange detailExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/blog/articles/1").build());
        MockServerWebExchange searchExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/blog/search?keyword=ai").build());

        filter.filter(listExchange, current -> Mono.empty()).block();
        filter.filter(detailExchange, current -> Mono.empty()).block();
        filter.filter(searchExchange, current -> Mono.empty()).block();

        assertThat(listExchange.getResponse().getStatusCode()).isNull();
        assertThat(detailExchange.getResponse().getStatusCode()).isNull();
        assertThat(searchExchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsProtectedRouteWithoutBearerToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(path -> false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/blog/articles").build());

        filter.filter(exchange, current -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsPublicAiChatButProtectsAgentWrites() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(path -> false);
        MockServerWebExchange chatExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/ai/agent/chat").build());
        MockServerWebExchange documentExchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/ai/agent/documents").build());

        filter.filter(chatExchange, current -> Mono.empty()).block();
        filter.filter(documentExchange, current -> Mono.empty()).block();

        assertThat(chatExchange.getResponse().getStatusCode()).isNull();
        assertThat(documentExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsProtectedRouteWithValidBearerToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(path -> true);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/blog/articles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build());

        filter.filter(exchange, current -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
}

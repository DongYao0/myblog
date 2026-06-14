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
    void rejectsProtectedRouteWithoutBearerToken() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(path -> false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/blog/articles").build());

        filter.filter(exchange, current -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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

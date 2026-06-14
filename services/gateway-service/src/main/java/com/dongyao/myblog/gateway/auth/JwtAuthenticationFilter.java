package com.dongyao.myblog.gateway.auth;

import java.util.function.Predicate;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final Predicate<String> tokenVerifier;

    @Autowired
    public JwtAuthenticationFilter(GatewayTokenVerifier tokenVerifier) {
        this(tokenVerifier::isValid);
    }

    JwtAuthenticationFilter(Predicate<String> tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (isPublic(path, method) || method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ") || !tokenVerifier.test(header.substring(7))) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublic(String path, HttpMethod method) {
        return path.equals("/api/blog/health")
                || path.startsWith("/api/blog/auth/")
                || (method == HttpMethod.GET && path.startsWith("/api/blog/articles"))
                || (method == HttpMethod.GET && path.startsWith("/api/blog/search"))
                || (method == HttpMethod.POST && path.equals("/api/ai/agent/chat"))
                || path.startsWith("/actuator/");
    }
}

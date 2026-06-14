package com.dongyao.myblog.blog.auth;

public record AuthResponse(Long userId, String username, String token) {
}

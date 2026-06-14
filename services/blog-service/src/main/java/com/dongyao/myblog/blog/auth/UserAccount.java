package com.dongyao.myblog.blog.auth;

public record UserAccount(Long id, String username, String passwordHash, String role) {
}

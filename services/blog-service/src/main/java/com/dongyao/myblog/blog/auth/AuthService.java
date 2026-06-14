package com.dongyao.myblog.blog.auth;

import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthService(UserRepository users, PasswordHasher passwordHasher, TokenService tokenService) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        validate(request.username(), request.password());
        users.findByUsername(request.username()).ifPresent(user -> {
            throw new IllegalArgumentException("username already exists");
        });
        UserAccount user = users.save(request.username(), passwordHasher.hash(request.password()));
        return new AuthResponse(user.id(), user.username(), tokenService.issue(user));
    }

    public AuthResponse login(LoginRequest request) {
        validate(request.username(), request.password());
        UserAccount user = users.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("invalid username or password"));
        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new IllegalArgumentException("invalid username or password");
        }
        return new AuthResponse(user.id(), user.username(), tokenService.issue(user));
    }

    private static void validate(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("password length must be at least 6");
        }
    }
}

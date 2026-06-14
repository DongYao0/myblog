package com.dongyao.myblog.blog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    @Test
    void registersUserAndReturnsLoginToken() {
        FakeUserRepository users = new FakeUserRepository();
        AuthService auth = new AuthService(users, new PasswordHasher(), new TokenService("test-secret"));

        AuthResponse registered = auth.register(new RegisterRequest("commander", "secret123"));
        AuthResponse loggedIn = auth.login(new LoginRequest("commander", "secret123"));

        assertThat(registered.username()).isEqualTo("commander");
        assertThat(loggedIn.token()).startsWith("myblog.");
        assertThat(users.findByUsername("commander")).isPresent();
    }

    @Test
    void rejectsDuplicateUsername() {
        FakeUserRepository users = new FakeUserRepository();
        AuthService auth = new AuthService(users, new PasswordHasher(), new TokenService("test-secret"));
        auth.register(new RegisterRequest("commander", "secret123"));

        assertThatThrownBy(() -> auth.register(new RegisterRequest("commander", "another123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username already exists");
    }

    private static final class FakeUserRepository implements UserRepository {
        private long sequence = 1;
        private final Map<String, UserAccount> users = new HashMap<>();

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public UserAccount save(String username, String passwordHash) {
            UserAccount user = new UserAccount(sequence++, username, passwordHash, "USER");
            users.put(username, user);
            return user;
        }
    }
}

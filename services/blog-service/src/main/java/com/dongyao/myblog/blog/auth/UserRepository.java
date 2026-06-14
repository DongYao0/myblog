package com.dongyao.myblog.blog.auth;

import java.util.Optional;

public interface UserRepository {
    Optional<UserAccount> findByUsername(String username);

    UserAccount save(String username, String passwordHash);
}

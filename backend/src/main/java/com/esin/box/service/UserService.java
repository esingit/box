package com.esin.box.service;

import com.esin.box.entity.User;

import java.time.LocalDateTime;
import java.util.Map;

public interface UserService {
    User findByUsername(String username);

    boolean register(User user);

    // 登录改为返回 Map，包含 accessToken 和 refreshToken
    Map<String, String> login(String username, String password);

    boolean checkPassword(User user, String rawPassword);

    boolean updatePassword(User user, String newPassword);

    void recordLogin(Long userId, LocalDateTime lastLoginTime); // 必须保留
}

package com.esin.box.service;

import com.esin.box.entity.User;

import java.time.LocalDateTime;
import java.util.Map;

public interface UserService {
    User findByUsername(String username);

    boolean register(User user);

    boolean checkPassword(User user, String rawPassword); // 返回true表示密码匹配，false表示不匹配

    boolean updatePassword(User user, String newPassword);

    void recordLogin(Long userId, LocalDateTime lastLoginTime); // 必须保留

    Map<String, String> generateLoginTokens(User user);
}

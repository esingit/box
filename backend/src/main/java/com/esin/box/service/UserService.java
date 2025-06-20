package com.esin.box.service;

import com.esin.box.entity.User;
import java.time.LocalDateTime;

public interface UserService {
    User findByUsername(String username);
    boolean register(User user);
    String login(String username, String password);
    boolean checkPassword(User user, String rawPassword);
    boolean updatePassword(User user, String newPassword);
    void recordLogin(Long userId, LocalDateTime lastLoginTime); // 必须有这行
}
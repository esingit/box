package com.box.main.service;

import com.box.main.entity.User;

public interface UserService {
    User findByUsername(String username);
    boolean register(User user);
    String login(String username, String password);
    boolean checkPassword(User user, String rawPassword);
    boolean updatePassword(User user, String newPassword);
}
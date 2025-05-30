package com.box.main.dto;

import java.time.LocalDateTime;

public class UserLoginResponseDTO {
    private String token;
    private String username;
    private String email;
    private LocalDateTime lastLoginTime;

    public UserLoginResponseDTO(String token, String username, String email, LocalDateTime lastLoginTime) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.lastLoginTime = lastLoginTime;
    }

    // Getters
    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    // Setters
    public void setToken(String token) {
        this.token = token;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
} 
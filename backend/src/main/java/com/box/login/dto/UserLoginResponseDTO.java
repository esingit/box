package com.box.login.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginResponseDTO {
    private String token;
    private String username;
    private String email;
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    public UserLoginResponseDTO(String token, String username, String email, LocalDateTime lastLoginTime) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.lastLoginTime = lastLoginTime;
    }
    }
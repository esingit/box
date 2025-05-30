package com.box.main.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String email;
    private java.time.LocalDateTime lastLoginTime;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public java.time.LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(java.time.LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}
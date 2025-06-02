package com.box.login.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {
    @TableId
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    }
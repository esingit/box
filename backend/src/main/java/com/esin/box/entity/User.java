package com.esin.box.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "user", autoResultMap = true)
public class User extends BaseEntity {
    @TableId
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id; // 主键ID

    private String username; // 用户名

    private String password; // 密码

    private String nickname; // 昵称

    private String email; // 邮箱

    private String phone; // 手机号

    private LocalDateTime lastLoginTime; // 最后登录时间

    }
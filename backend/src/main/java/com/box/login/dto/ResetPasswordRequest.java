package com.box.login.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String username;
    private String oldPassword;
    private String newPassword;

    }
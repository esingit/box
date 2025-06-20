package com.esin.box.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String username;
    private String oldPassword;
    private String newPassword;

    }
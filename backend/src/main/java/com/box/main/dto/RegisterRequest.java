package com.box.main.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private Long id;
    private String username;
    private String password;
    private String captcha;
    private String captchaId;
}
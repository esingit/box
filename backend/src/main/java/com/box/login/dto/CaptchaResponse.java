package com.box.login.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaptchaResponse {
    private String captchaId;
    private String captchaUrl;

    public CaptchaResponse(String captchaId, String captchaUrl) {
        this.captchaId = captchaId;
        this.captchaUrl = captchaUrl;
    }
}

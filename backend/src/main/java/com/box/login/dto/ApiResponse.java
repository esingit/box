package com.box.login.dto;

import lombok.Data;

@Data
public class ApiResponse {
    private boolean success;
    private String message;
    private Object data;
    private boolean showCaptcha;

    public ApiResponse(boolean success, String message, Object data, boolean showCaptcha) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.showCaptcha = showCaptcha;
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.showCaptcha = false;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public boolean isShowCaptcha() { return showCaptcha; }
}
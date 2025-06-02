package com.box.login.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private boolean showCaptcha;

    public ApiResponse(boolean success, String message, T data, boolean showCaptcha) {
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
    public T getData() { return data; }
    public boolean isShowCaptcha() { return showCaptcha; }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "success", data, false);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "success", null, false);
    }
}
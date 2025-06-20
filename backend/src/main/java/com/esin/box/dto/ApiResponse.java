package com.esin.box.dto;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private boolean needCaptcha;

    public ApiResponse(boolean success, String message, T data, boolean needCaptcha) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.needCaptcha = needCaptcha;
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
        this.needCaptcha = false;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public boolean isNeedCaptcha() { return needCaptcha; }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "success", data, false);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "success", null, false);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, false);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data, false);
    }
}
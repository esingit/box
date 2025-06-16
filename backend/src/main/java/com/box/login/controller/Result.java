package com.box.login.controller;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Result<T> {
    private boolean success;
    private String message;
    private T data;
    private boolean needCaptcha;

    public Result() {
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setSuccess(true)
                .setMessage("操作成功")
                .setData(data);
    }

    public static <T> Result<T> success() {
        return new Result<T>()
                .setSuccess(true)
                .setMessage("操作成功");
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>()
                .setSuccess(false)
                .setMessage(message);
    }

    public static <T> Result<T> error(String message, boolean needCaptcha) {
        return new Result<T>()
                .setSuccess(false)
                .setMessage(message)
                .setNeedCaptcha(needCaptcha);
    }

    public static <T> Result<T> of(boolean success, String message, T data) {
        return new Result<T>()
                .setSuccess(success)
                .setMessage(message)
                .setData(data);
    }
}

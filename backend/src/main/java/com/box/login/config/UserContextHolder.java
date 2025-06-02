package com.box.login.config;

public class UserContextHolder {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void set(String username) {
        currentUser.set(username);
    }

    public static String get() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}

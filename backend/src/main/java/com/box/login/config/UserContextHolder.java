package com.box.login.config;

import org.springframework.core.NamedThreadLocal;

public class UserContextHolder {
    private static final ThreadLocal<String> userHolder = new NamedThreadLocal<>("Current User");

    public static void setCurrentUser(String username) {
        userHolder.set(username);
    }

    public static String getCurrentUsername() {
        return userHolder.get();
    }

    public static void clear() {
        userHolder.remove();
    }
}

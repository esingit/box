package com.box.login.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private String getCurrentUsername() {
        try {
            String username = UserContextHolder.get();
            if (username == null || "anonymousUser".equals(username)) return null;
            return username;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "version", Integer.class, 1);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        String username = getCurrentUsername();
        if (username != null) {
            this.strictInsertFill(metaObject, "createUser", String.class, username);
            this.strictInsertFill(metaObject, "updateUser", String.class, username);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        String username = getCurrentUsername();
        if (username != null) {
            this.strictUpdateFill(metaObject, "updateUser", String.class, username);
        }
    }
}
package com.esin.box.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.esin.box.entity.User;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private String getCurrentUsername() {
        try {
            String username = UserContextHolder.getCurrentUsername();
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
        
        // 获取实体对象
        String createUser = null;
        if (metaObject.getOriginalObject() instanceof User) {
            User user = (User) metaObject.getOriginalObject();
            createUser = user.getUsername();  // 如果是用户注册，使用注册的用户名
        } else {
            createUser = getCurrentUsername();  // 其他情况使用当前用户名
        }
        
        // 如果还是null，使用默认值
        if (createUser == null) {
            createUser = "system";
        }
        
        this.strictInsertFill(metaObject, "createUser", String.class, createUser);
        this.strictInsertFill(metaObject, "updateUser", String.class, createUser);
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
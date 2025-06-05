package com.box.login.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private String header = "Authorization";  // 默认值为 "Authorization"

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}

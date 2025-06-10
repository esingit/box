package com.box.login.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    private String secret;
    private long expiration = 7200000; // 默认2小时
    private long refreshWindow = 1800000; // 默认30分钟
    private long blacklistExpiration = 86400000; // 默认24小时

    // Redis key前缀配置
    private String activeTokenPrefix = "active_token:";
    private String blacklistPrefix = "token_blacklist:";
}

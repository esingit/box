package com.box.main.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.box.main.mapper")
public class MyBatisPlusConfig {
    // 后续可配置分页插件等
}
package com.box.login.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI springOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("资产管理系统 API")
                        .description("资产管理系统后端接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("support@example.com")));
    }
}

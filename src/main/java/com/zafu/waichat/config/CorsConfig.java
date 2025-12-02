package com.zafu.waichat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有接口
                .allowedOrigins("http://localhost:5173") // 允许前端来源
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 允许请求方法
                .allowCredentials(true) // 允许携带 Cookie
                .maxAge(3600); // 预检请求有效期
    }
}
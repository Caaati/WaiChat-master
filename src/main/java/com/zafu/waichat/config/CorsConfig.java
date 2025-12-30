package com.zafu.waichat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

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
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传文件的静态资源映射
        String projectPath = System.getProperty("user.dir");
        // 注意末尾必须有斜杠，且 windows 环境下需要 file: 前缀
        String path = "file:" + projectPath + File.separator + "uploads" + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(path);
        System.out.println("静态资源映射路径: " + path);
    }
}

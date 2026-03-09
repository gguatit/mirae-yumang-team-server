package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 브라우저 주소창에 /upload/** 로 들어오는 요청을
        // application.properties에 설정된 폴더로 연결합니다.
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file://" + uploadDir); 
                // ⚠️ uploadDir는 반드시 '/'로 끝나야 합니다!
    }
}
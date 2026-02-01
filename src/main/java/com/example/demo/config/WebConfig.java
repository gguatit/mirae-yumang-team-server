package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 브라우저 주소창에 /upload/** 로 들어오는 요청을
        // 실제 내 컴퓨터의 C:/starlog/upload/ 폴더로 연결합니다.
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:///C:/starlog/upload/"); 
                // ⚠️ 마지막에 반드시 '/'가 있어야 합니다!
    }
}
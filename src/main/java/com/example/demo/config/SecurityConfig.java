package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 기존 세션 기반 수동 인증을 유지하면서 보안 기능만 활성화
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // CSRF 보호 활성화 (H2 콘솔은 예외)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**"))
                // H2 콘솔의 iframe 표시를 위한 설정
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                // Spring Security 기본 로그인 페이지 비활성화 (수동 인증 사용)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

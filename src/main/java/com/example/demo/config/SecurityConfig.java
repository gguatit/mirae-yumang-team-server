package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

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
                // 보안 헤더 설정
                .headers(headers -> headers
                        // H2 콘솔의 iframe 표시를 위한 설정
                        .frameOptions(frame -> frame.sameOrigin())
                        // MIME 스니핑 방지
                        .contentTypeOptions(contentType -> {})
                        // HSTS (HTTPS 강제) - 1년 유지
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        // Content-Security-Policy: 인라인 스크립트 및 외부 리소스 제한
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline' 'unsafe-eval' cdnjs.cloudflare.com static.cloudflareinsights.com esm.sh unpkg.com challenges.cloudflare.com; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: blob: app.spline.design prod.spline.design; " +
                                        "font-src 'self' cdn.jsdelivr.net; " +
                                        "connect-src 'self' cloudflareinsights.com esm.sh prod.spline.design app.spline.design challenges.cloudflare.com; " +
                                        "frame-src 'self' challenges.cloudflare.com; " +
                                        "frame-ancestors 'self'"))
                        // Referrer-Policy: 외부 사이트로 URL 정보 누출 방지
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
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

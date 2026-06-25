package com.example.demo.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 세션 기반 수동 인증 유지하면서 Spring Security를 2차 방어선으로 사용
                // 공개 경로만 permitAll, 나머지는 컨트롤러의 수동 체크와 무관하게 인증 요구
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/home"),
                                new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/upload/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/error"),
                                // 인증
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/register"),
                                // 학교 시간표/급식
                                new AntPathRequestMatcher("/school"),
                                // 게시글 목록/상세 (조회)
                                new AntPathRequestMatcher("/posts"),
                                new AntPathRequestMatcher("/posts/{id:[\\d]+}"),
                                new AntPathRequestMatcher("/posts/api/new"),
                                new AntPathRequestMatcher("/posts/api/best"),
                                new AntPathRequestMatcher("/h2-console/**")
                        ).permitAll()
                        .anyRequest().authenticated())
                // 인증되지 않은 사용자가 보호 라우트 접근 시 /auth/login으로 리다이렉트
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            sendRedirectToLogin(request, response);
                        }))
                // CSRF 보호 활성화 (H2 콘솔, 로그인/회원가입 POST는 예외)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/h2-console/**",
                                "/auth/login",
                                "/auth/register"
                        ))
                // 보안 헤더 설정
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentTypeOptions(contentType -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
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
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    private void sendRedirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // AJAX 요청은 401 JSON으로 응답, 그 외는 /auth/login으로 리다이렉트
        String accept = request.getHeader("Accept");
        String xrw = request.getHeader("X-Requested-With");
        boolean isAjax = (accept != null && accept.contains("application/json"))
                || (xrw != null && xrw.equalsIgnoreCase("XMLHttpRequest"))
                || request.getRequestURI().contains("/api/");
        if (isAjax) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"unauthorized\"}");
        } else {
            response.sendRedirect("/auth/login");
        }
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

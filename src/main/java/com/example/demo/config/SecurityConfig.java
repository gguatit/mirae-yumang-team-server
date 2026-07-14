package com.example.demo.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CustomOAuth2UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService, UserRepository userRepository, Environment env) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/home"),
                                new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/upload/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                new AntPathRequestMatcher("/error"),
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/register"),
                                new AntPathRequestMatcher("/school"),
                                new AntPathRequestMatcher("/posts"),
                                new AntPathRequestMatcher("/posts/**"),
                                new AntPathRequestMatcher("/comments/**"),
                                new AntPathRequestMatcher("/mypage"),
                                new AntPathRequestMatcher("/mypage/**"),
                                new AntPathRequestMatcher("/auth/**"),
                                new AntPathRequestMatcher("/h2-console/**"),
                                new AntPathRequestMatcher("/oauth2/**")
                        ).permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            sendRedirectToLogin(request, response);
                        }))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/h2-console/**",
                                "/auth/login",
                                "/auth/register"
                        ))
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
                                "style-src 'self' 'unsafe-inline' cdn.jsdelivr.net; " +
                                "img-src 'self' data: blob: app.spline.design prod.spline.design https://starlog.c01.kr https://avatars.githubusercontent.com; " +
                                "font-src 'self' cdn.jsdelivr.net https://cdn.jsdelivr.net; " +
                                "connect-src 'self' https://starlog.c01.kr http://starlog.c01.kr cloudflareinsights.com esm.sh prod.spline.design app.spline.design challenges.cloudflare.com; " +
                                "frame-src 'self' challenges.cloudflare.com; " +
                                "frame-ancestors 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/auth/login")
                        .userInfoEndpoint(ui -> ui
                                .userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            var oAuth2User = (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
                            Map<String, Object> attributes = oAuth2User.getAttributes();
                            String registrationId = "";
                            if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token) {
                                registrationId = token.getAuthorizedClientRegistrationId();
                            }
                            String providerId = String.valueOf(attributes.get("id"));
                            String login = (String) attributes.get("login");
                            String email = (String) attributes.get("email");
                            if (email == null || email.isBlank()) {
                                email = login + "@github.com";
                            }

                            HttpSession oldSession = request.getSession(false);
                            Long oldUserId = null;
                            if (oldSession != null) {
                                oldUserId = (Long) oldSession.getAttribute("userId");
                                oldSession.invalidate();
                            }

                            com.example.demo.entity.User user = userRepository.findByProviderAndProviderId(registrationId, providerId).orElse(null);

                            if (user == null && oldUserId != null) {
                                user = userRepository.findById(oldUserId).orElse(null);
                                if (user != null) {
                                    user.setProvider(registrationId);
                                    user.setProviderId(providerId);
                                    userRepository.save(user);
                                }
                            }

                            if (user == null) {
                                String baseUsername = login != null ? login : registrationId + "_" + providerId;
                                String username = baseUsername;
                                int suffix = 1;
                                while (userRepository.existsByUsername(username)) {
                                    username = baseUsername + suffix;
                                    suffix++;
                                }
                                user = new com.example.demo.entity.User(username, email, registrationId, providerId);
                                userRepository.save(user);
                            }

                            HttpSession session = request.getSession(true);
                            session.setAttribute("userId", user.getId());
                            session.setAttribute("loginUser", user.getUsername());
                            session.setAttribute("loginEmail", user.getEmail());
                            response.sendRedirect("/home");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("/auth/login?error=oauth");
                        }))
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

package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    // ============================================
    // 홈 페이지
    // ============================================

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        return home(session, model);
    }

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");

        if (username != null) {
            model.addAttribute("username", username);
            log.info("홈 접속: {} (로그인 상태)", username);
        } else {
            log.info("홈 접속: 비로그인 상태");
        }

        return "home";
    }

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");

        if (username == null) {
            log.warn("비로그인 사용자가 마이페이지 접근 시도");
            return "redirect:/auth/login";
        }

        // Service를 통해 사용자 정보 조회 (Repository 직접 접근 제거)
        User user = userService.getUserByUsername(username);

        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("createdAt", user.getCreatedAt());

        log.info("마이페이지 접속: {}", username);

        return "mypage";
    }
}
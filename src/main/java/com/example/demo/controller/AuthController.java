package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 📌 인증(Authentication) 관련 컨트롤러
 *
 * URL 구조:
 * - /auth/login : 로그인
 * - /auth/register : 회원가입
 * - /auth/logout : 로그아웃
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    // ============================================
    // 로그인 처리
    // ============================================

    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/home";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request,
            Model model) {
        log.info("로그인 시도: {}", username);

        User user = userService.authenticateUser(username, password);

        if (user == null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login";
        }

        // 세션 고정 공격 방지: 기존 세션의 속성을 보존하면서 세션 ID만 변경
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setAttribute("loginUser", username);
        session.setAttribute("loginEmail", user.getEmail());

        log.info("로그인 성공! 세션ID: {}", session.getId());

        return "redirect:/home";
    }

    // ============================================
    // 회원가입 처리
    // ============================================

    @GetMapping("/register")
    public String showRegisterForm(HttpSession session) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/home";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("email") String email,
            Model model) {
        log.info("회원가입 시도: {}", username);

        // 입력값 검증
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "아이디를 입력해주세요.");
            return "register";
        }

        if (password == null || password.length() < 4) {
            model.addAttribute("error", "비밀번호는 4자 이상이어야 합니다.");
            return "register";
        }

        boolean success = userService.registerUser(username, password, email);

        if (!success) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "register";
        }

        log.info("회원가입 성공: {}", username);

        model.addAttribute("message", "회원가입이 완료되었습니다! 로그인해주세요.");
        return "login";
    }

    // ============================================
    // 로그아웃 처리
    // ============================================

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        String username = (String) session.getAttribute("loginUser");
        log.info("로그아웃: {}", username);

        session.invalidate();
        log.info("세션 삭제 완료");

        return "redirect:/auth/login";
    }
}
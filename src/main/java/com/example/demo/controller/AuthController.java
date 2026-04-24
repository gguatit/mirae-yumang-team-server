package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.LoginAttemptService;
import com.example.demo.service.TurnstileService;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final LoginAttemptService loginAttemptService;
    private final TurnstileService turnstileService;

    @Value("${turnstile.site.key}")
    private String turnstileSiteKey;

    private String getClientKey(HttpServletRequest request, String username) {
        String ip = getClientIp(request);
        return ip + ":" + username;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // ============================================
    // 로그인 처리
    // ============================================

    @GetMapping("/login")
    public String showLoginForm(HttpSession session, Model model) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/home";
        }
        model.addAttribute("turnstileSiteKey", turnstileSiteKey);
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "cf-turnstile-response", required = false) String turnstileToken,
            HttpServletRequest request,
            Model model) {
        log.info("로그인 시도: {}", username);

        // Turnstile 검증
        String clientIp = getClientIp(request);
        if (!turnstileService.validateToken(turnstileToken, clientIp)) {
            model.addAttribute("error", "보안 인증에 실패했습니다. 다시 시도해주세요.");
            return "login";
        }

        String clientKey = getClientKey(request, username);

        // 브루트포스 잠금 여부 확인
        if (loginAttemptService.isLocked(clientKey)) {
            log.warn("로그인 잠금 상태: {}", clientKey);
            model.addAttribute("error", "로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해주세요.");
            return "login";
        }

        User user = userService.authenticateUser(username, password);

        if (user == null) {
            loginAttemptService.recordFailure(clientKey);
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login";
        }

        // 로그인 성공: 실패 횟수 초기화
        loginAttemptService.clearFailures(clientKey);

        // 세션 고정 공격 방지: 기존 세션 무효화 후 새 세션 발급
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
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
    public String showRegisterForm(HttpSession session, Model model) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/home";
        }
        model.addAttribute("turnstileSiteKey", turnstileSiteKey);
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("email") String email,
            @RequestParam(value = "cf-turnstile-response", required = false) String turnstileToken,
            HttpServletRequest request,
            Model model) {
        log.info("회원가입 시도: {}", username);

        // Turnstile 검증
        String clientIp = getClientIp(request);
        if (!turnstileService.validateToken(turnstileToken, clientIp)) {
            model.addAttribute("error", "보안 인증에 실패했습니다. 다시 시도해주세요.");
            return "register";
        }

        // 입력값 검증
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "아이디를 입력해주세요.");
            return "register";
        }
        
        if (username.length() < 3 || username.length() > 20) {
            model.addAttribute("error", "아이디는 3~20자로 입력해주세요.");
            return "register";
        }
        
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            model.addAttribute("error", "아이디는 영문, 숫자, 언더스코어만 사용 가능합니다.");
            return "register";
        }

        if (password == null || password.length() < 8) {
            model.addAttribute("error", "비밀번호는 8자 이상이어야 합니다.");
            return "register";
        }
        
        if (password.length() > 100) {
            model.addAttribute("error", "비밀번호는 100자를 초과할 수 없습니다.");
            return "register";
        }
        
        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("error", "이메일을 입력해주세요.");
            return "register";
        }
        
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            model.addAttribute("error", "올바른 이메일 형식이 아닙니다.");
            return "register";
        }

        // 이메일 중복 검사 (별도 에러 메시지 제공)
        if (userService.existsByEmail(email)) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
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

    // ============================================
    // 회원 탈퇴 처리
    // ============================================

    @GetMapping("/delete-account")
    public String showDeleteAccountForm(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/auth/login";
        }
        return "delete-account";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpSession session,
            Model model) {

        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        // DoS 방지: 비밀번호 길이 제한 (BCrypt는 72바이트 초과 입력 시 부하 유발)
        if (password == null || password.length() > 100) {
            model.addAttribute("error", "비밀번호가 올바르지 않습니다.");
            return "delete-account";
        }

        // 브루트포스 방지: 로그인과 동일한 시도 횟수 제한 적용
        String clientKey = getClientKey(request, username);
        if (loginAttemptService.isLocked(clientKey)) {
            log.warn("회원 탈퇴 - 잠금 상태: {}", clientKey);
            model.addAttribute("error", "시도 횟수를 초과했습니다. 15분 후 다시 시도해주세요.");
            return "delete-account";
        }

        log.info("회원 탈퇴 시도: {}", username);

        boolean success = userService.deleteUser(username, password);

        if (!success) {
            loginAttemptService.recordFailure(clientKey);
            model.addAttribute("error", "비밀번호가 올바르지 않습니다.");
            return "delete-account";
        }

        // 탈퇴 완료 → 실패 기록 초기화 및 세션 무효화
        loginAttemptService.clearFailures(clientKey);
        session.invalidate();
        log.info("회원 탈퇴 완료 및 세션 삭제: {}", username);

        return "redirect:/auth/login?deleted=true";
    }
}
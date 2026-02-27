package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 📌 인증(Authentication) 관련 컨트롤러
 * 
 * @RequestMapping("/auth")를 사용하는 이유:
 * 1. URL 구조화: 인증 관련 기능을 /auth 하위로 그룹화
 * - /auth/login : 로그인
 * - /auth/register : 회원가입
 * - /auth/logout : 로그아웃
 * 
 * 2. RESTful 설계: 기능별로 URL을 계층적으로 구성
 * - /auth/* : 인증 관련
 * - /posts/* : 게시글 관련
 * - /users/* : 사용자 관련 (만약 추가한다면)
 * 
 * 3. 보안 설정 용이: Spring Security 적용 시 경로별 권한 설정이 쉬워짐
 * 예) /auth/** 는 모두 permitAll()
 * /admin/** 는 ROLE_ADMIN만 접근 가능
 * 
 * 4. 유지보수성: 나중에 API를 추가할 때도 일관된 구조 유지
 * 예) /api/auth/login (REST API용)
 */
@Controller
@RequestMapping("/auth") // 이 컨트롤러의 모든 메서드는 /auth로 시작
public class AuthController {

    @Autowired
    private UserService userService;

    // ============================================
    // 로그인 처리
    // ============================================

    /**
     * 로그인 폼 표시
     * 실제 URL: /auth/login (GET)
     * 
     * @GetMapping("/login")은 @RequestMapping 위에 선언된 "/auth"와 결합되어
     * 최종적으로 "/auth/login" 경로로 매핑됩니다.
     */
    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        // 이미 로그인된 사용자는 홈으로 리다이렉트
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/home";
        }
        return "login"; // templates/login.html 렌더링
    }

    /**
     * 로그인 처리
     * 실제 URL: /auth/login (POST)
     * 
     * HTML form의 action="/auth/login" method="post"와 연결됩니다.
     */
    @PostMapping("/login")
    public String login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {
        System.out.println("🔐 로그인 시도: " + username);

        // 1. 사용자 인증 (UserService 활용)
        User user = userService.authenticateUser(username, password);

        if (user == null) {
            // 로그인 실패
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "login"; // login.html로 다시 이동
        }

        // [중요!] 이 줄이 있어야 PostController에서 userId를 꺼낼 수 있습니다.
        session.setAttribute("userId", user.getId());

        // 2. 로그인 성공 → 세션에 사용자 정보 저장
        session.setAttribute("loginUser", username); // 핵심!
        session.setAttribute("loginEmail", user.getEmail());

        System.out.println("로그인 성공! 세션ID: " + session.getId());
        System.out.println("세션에 저장된 사용자: " + session.getAttribute("loginUser"));

        // 3. 홈 화면으로 리다이렉트
        return "redirect:/home";
    }

    // ============================================
    // 회원가입 처리
    // ============================================

    /**
     * 회원가입 폼 표시
     * 실제 URL: /auth/register (GET)
     */
    @GetMapping("/register")
    public String showRegisterForm(HttpSession session) {
        // 이미 로그인된 경우 홈으로 이동
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/home";
        }
        return "register"; // templates/register.html 렌더링
    }

    /**
     * 회원가입 처리
     * 실제 URL: /auth/register (POST)
     * 
     * @RequestParam으로 form 데이터를 받습니다:
     *                 - username: 사용자 아이디
     *                 - password: 비밀번호
     *                 - email: 이메일
     */
    @PostMapping("/register")
    public String register(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("email") String email,
            Model model) {
        System.out.println("회원가입 시도: " + username);

        // 1. 입력값 검증
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "아이디를 입력해주세요.");
            return "register";
        }

        if (password == null || password.length() < 4) {
            model.addAttribute("error", "비밀번호는 4자 이상이어야 합니다.");
            return "register";
        }

        // 2. 회원가입 처리 (UserService 활용)
        boolean success = userService.registerUser(username, password, email);

        if (!success) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "register";
        }

        System.out.println("회원가입 성공: " + username);

        // 3. 로그인 페이지로 이동 (성공 메시지 포함)
        model.addAttribute("message", "회원가입이 완료되었습니다! 로그인해주세요.");
        return "login";
    }

    // ============================================
    // 로그아웃 처리
    // ============================================

    /**
     * 로그아웃 처리
     * 실제 URL: /auth/logout (POST)
     * 
     * 보안상 로그아웃은 POST 방식을 권장합니다.
     * (CSRF 공격 방지)
     */
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        String username = (String) session.getAttribute("loginUser");
        System.out.println("로그아웃: " + username);

        // 세션 무효화 (모든 데이터 삭제)
        session.invalidate();

        System.out.println("세션 삭제 완료");

        return "redirect:/auth/login";
    }

    // GET 방식 로그아웃은 보안상 제거됨 (POST만 허용)

}
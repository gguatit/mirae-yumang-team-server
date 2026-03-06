package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;


@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;


    @param session 현재 사용자의 세션 (Spring이 자동 주입)
    @param model 뷰에 데이터를 전달하기 위한 객체
    @throws Exception 

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        return home(session, model);
    }

    /**
     * 홈 페이지 표시
     * 
     * 학습 포인트:
     * - 로그인 여부에 따라 다른 화면 표시 (동일 템플릿, 다른 데이터)
     * - Thymeleaf의 th:if를 활용한 조건부 렌더링
     * @throws Exception 
     */
    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        // 세션에서 로그인 정보 확인
        // getAttribute()는 Object를 반환하므로 String으로 캐스팅 필요
        String username = (String) session.getAttribute("loginUser");
        
        if (username != null) {
            // 로그인 상태: username을 뷰에 전달
            model.addAttribute("username", username);
            System.out.println("홈 접속: " + username + " (로그인 상태)");
        } else {
            // 비로그인 상태
            System.out.println("홈 접속: 비로그인 상태");
        }

        return "home";  // templates/home.html 렌더링
    }

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        // 1. 세션 확인: 로그인 여부 체크
        String username = (String) session.getAttribute("loginUser");

        if (username == null) {
            // 로그인하지 않은 경우 → 로그인 페이지로 리다이렉트
            System.out.println("❌ 비로그인 사용자가 마이페이지 접근 시도");
            return "redirect:/auth/login";
        }

        // 2. DB에서 사용자 정보 조회 (createdAt 등 추가 정보 포함)
        // Optional<User>를 반환하므로 orElseThrow()로 안전하게 처리
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("DB에 존재하지 않는 사용자: " + username));

        // 3. 로그인한 경우: 사용자 데이터를 뷰에 전달
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("createdAt", user.getCreatedAt());

        System.out.println("마이페이지 접속: " + username);

        return "mypage";  // templates/mypage.html 렌더링
    }
}
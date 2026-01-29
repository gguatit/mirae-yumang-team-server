package com.example.demo.controller;

import com.example.demo.crawler.CrawlingUtils;
import com.example.demo.entity.Unsae;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 📌 홈 페이지 및 마이페이지 컨트롤러
 * 
 * 학습 포인트:
 * 1. @Autowired를 통한 의존성 주입 (Dependency Injection)
 * 2. HttpSession을 활용한 로그인 상태 관리
 * 3. Repository를 직접 주입하는 방식 vs Service를 통한 방식 비교
 * 
 * 📚 설계 선택지:
 * - HomeController는 UserRepository를 직접 주입받아 사용
 * - AuthController는 UserService를 통해 간접적으로 사용
 * 
 * 어떤 방식이 더 좋을까요?
 * → 일반적으로 Service를 사용하는 것이 권장됩니다!
 *   (비즈니스 로직 분리, 트랜잭션 관리, 재사용성 등)
 * → 하지만 간단한 조회만 할 경우 Repository 직접 사용도 가능합니다.
 */
@Controller
public class HomeController {

    /**
     * @Autowired: Spring이 자동으로 UserRepository 구현체를 주입
     * 
     * 학습 포인트:
     * - Spring Boot는 JpaRepository를 상속받은 인터페이스를 자동으로 구현
     * - 개발자가 구현 클래스를 만들지 않아도 CRUD 기능 사용 가능
     * - 이것이 Spring Data JPA의 핵심 기능!
     */
    @Autowired
    private UserRepository userRepository;

    // ============================================
    // 홈 페이지
    // ============================================

    /**
     * 루트 경로("/") 접속 시 홈으로 리다이렉트
     * 
     * @param session 현재 사용자의 세션 (Spring이 자동 주입)
     * @param model 뷰에 데이터를 전달하기 위한 객체
     * @throws Exception 
     */
    @GetMapping("/")
    public String index(HttpSession session, Model model) throws Exception {
        return home(session, model);  // home() 메서드 재활용
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
    public String home(HttpSession session, Model model) throws Exception {
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


    // ============================================
    // 마이페이지 (로그인 필수!)
    // ============================================

    /**
     * 마이페이지 조회
     * 
     * 학습 포인트:
     * 1. 세션 기반 인증 확인
     * 2. 로그인하지 않은 사용자는 로그인 페이지로 리다이렉트
     * 3. DB에서 최신 사용자 정보 조회
     * 4. Optional을 활용한 안전한 데이터 처리
     * 
     * 💡 왜 세션 정보만으로는 부족할까?
     * - 세션: 일부 정보만 저장 (username, email 정도)
     * - DB 조회: createdAt(가입일) 등 추가 정보 필요
     * - 또한 DB 정보가 변경되었을 수 있으므로 최신 정보 확인
     */
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
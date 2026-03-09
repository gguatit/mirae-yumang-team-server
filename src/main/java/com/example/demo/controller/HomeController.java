package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.service.PostService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final PostService postService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

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

    // ============================================
    // 마이페이지 조회
    // ============================================

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");

        if (username == null) {
            log.warn("비로그인 사용자가 마이페이지 접근 시도");
            return "redirect:/auth/login";
        }

        User user = userService.getUserByUsername(username);
        List<Post> myPosts = postService.getMyPosts(username);
        long postCount = postService.getUserPostCount(username);

        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("createdAt", user.getCreatedAt());
        model.addAttribute("bio", user.getBio());
        model.addAttribute("profileImagePath", user.getProfileImagePath());
        model.addAttribute("myPosts", myPosts);
        model.addAttribute("postCount", postCount);

        log.info("마이페이지 접속: {}", username);
        return "mypage";
    }

    // ============================================
    // 자기소개 수정
    // ============================================

    @PostMapping("/mypage/bio")
    public String updateBio(
            @RequestParam("bio") String bio,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        if (bio != null && bio.length() > 500) {
            redirectAttributes.addFlashAttribute("error", "자기소개는 500자를 초과할 수 없습니다.");
            return "redirect:/mypage";
        }

        userService.updateBio(username, bio == null ? "" : bio.trim());
        redirectAttributes.addFlashAttribute("success", "자기소개가 저장되었습니다.");
        return "redirect:/mypage";
    }

    // ============================================
    // 프로필 이미지 업로드
    // ============================================

    @PostMapping("/mypage/profile-image")
    public String updateProfileImage(
            @RequestParam("profileImage") MultipartFile profileImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) throws IOException {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        if (profileImage == null || profileImage.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "파일을 선택해주세요.");
            return "redirect:/mypage";
        }

        if (profileImage.getSize() > 5 * 1024 * 1024) {
            redirectAttributes.addFlashAttribute("error", "파일 크기는 5MB를 초과할 수 없습니다.");
            return "redirect:/mypage";
        }

        String originalFilename = profileImage.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            redirectAttributes.addFlashAttribute("error", "유효하지 않은 파일명입니다.");
            return "redirect:/mypage";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            redirectAttributes.addFlashAttribute("error", "이미지 파일만 업로드 가능합니다. (jpg, png, gif, webp)");
            return "redirect:/mypage";
        }

        String mimeType = profileImage.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            redirectAttributes.addFlashAttribute("error", "유효하지 않은 파일 형식입니다.");
            return "redirect:/mypage";
        }

        File folder = new File(uploadDir);
        if (!folder.exists()) folder.mkdirs();

        String uuid = UUID.randomUUID().toString();
        String fileName = "profile_" + uuid + "." + extension;
        profileImage.transferTo(new File(folder, fileName));

        userService.updateProfileImage(username, "/upload/" + fileName);
        redirectAttributes.addFlashAttribute("success", "프로필 이미지가 변경되었습니다.");
        return "redirect:/mypage";
    }
}

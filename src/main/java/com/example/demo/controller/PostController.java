package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.RecommendationType;
import com.example.demo.service.LhService;
import com.example.demo.service.PostService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 📌 게시글(Post) 관련 컨트롤러
 *
 * URL 구조:
 * - GET /posts → 게시글 목록
 * - GET /posts/write → 글쓰기 폼
 * - POST /posts/write → 글쓰기 처리
 * - GET /posts/{id} → 게시글 상세 조회
 * - GET /posts/{id}/edit → 게시글 수정 폼
 * - POST /posts/{id}/edit → 게시글 수정 처리
 * - POST /posts/{id}/delete → 게시글 삭제
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    // 허용된 이미지 확장자 (보안)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp");

    // 허용된 MIME 타입 (보안 강화)
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final PostService postService;
    private final LhService lhService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ============================================
    // 게시글 목록
    // ============================================

    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "basic") String searchMode,
            HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("username", username);
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchMode", searchMode);

        // searchMode: basic (기본), enhanced (향상된 검색 - 초성/자모/영타)
        Page<Post> postsPage;
        if ("enhanced".equals(searchMode) && keyword != null && !keyword.isEmpty()) {
            postsPage = postService.getPagedPostsEnhanced(keyword, page);
            log.info("향상된 검색 모드: '{}'", keyword);
        } else {
            postsPage = postService.getPagedPosts(keyword, page);
        }
        
        model.addAttribute("postsPage", postsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());

        Page<Post> popularPage = postService.getPopularPosts(0);
        model.addAttribute("bestPosts", popularPage.getContent());

        return "post-list";
    }

    // ============================================
    // 게시글 작성 폼
    // ============================================

    @GetMapping("/write")
    public String writeForm(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            log.warn("비로그인 사용자가 글쓰기 시도");
            return "redirect:/auth/login";
        }

        model.addAttribute("username", username);
        return "post-write";
    }

    // ============================================
    // 게시글 작성 처리
    // ============================================

    @PostMapping("/write")
    public String write(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("imageFile") List<MultipartFile> imageFiles,
            HttpSession session,
            Model model) throws IOException {
        String username = (String) session.getAttribute("loginUser");

        if (username == null) {
            return "redirect:/auth/login";
        }

        // 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            model.addAttribute("error", "제목을 입력해주세요.");
            return "post-write";
        }
        
        if (title.length() > 200) {
            model.addAttribute("error", "제목은 200자를 초과할 수 없습니다.");
            return "post-write";
        }

        if (content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "내용을 입력해주세요.");
            return "post-write";
        }
        
        if (content.length() > 10000) {
            model.addAttribute("error", "내용은 10,000자를 초과할 수 없습니다.");
            return "post-write";
        }

        try {
            List<String> fileNames = new ArrayList<>();
            List<String> filePaths = new ArrayList<>();

            // uploadDir는 @Value로 주입받음
            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    
                    // 파일명 null 체크 및 기본 검증
                    if (originalFilename == null || originalFilename.trim().isEmpty() || !originalFilename.contains(".")) {
                        model.addAttribute("error", "유효하지 않은 파일명입니다.");
                        return "post-write";
                    }
                    
                    // 파일 크기 추가 검증 (10MB)
                    if (file.getSize() > 10 * 1024 * 1024) {
                        model.addAttribute("error", "파일 크기는 10MB를 초과할 수 없습니다.");
                        return "post-write";
                    }
                    
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

                    // 파일 확장자 검증
                    if (!ALLOWED_EXTENSIONS.contains(extension)) {
                        model.addAttribute("error", "허용되지 않는 파일 형식입니다. (jpg, png, gif, webp만 가능)");
                        return "post-write";
                    }

                    // MIME 타입 검증 (강화)
                    String mimeType = file.getContentType();
                    if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
                        log.error("유효하지 않은 MIME 타입: {} (파일: {})", mimeType, originalFilename);
                        model.addAttribute("error", "유효하지 않은 파일 형식입니다. 실제 이미지 파일만 업로드 가능합니다.");
                        return "post-write";
                    }

                    String uuid = UUID.randomUUID().toString();
                    String fileName = uuid + "." + extension;

                    file.transferTo(new File(uploadDir + fileName));

                    fileNames.add(fileName);
                    filePaths.add("/upload/" + fileName);

                    log.info("파일 저장 성공: {}", fileName);
                }
            }
            Post post = postService.createPost(title, content, username, fileNames, filePaths);
            log.info("게시글 작성 완료: {}", post.getId());
            return "redirect:/posts/" + post.getId();

        } catch (Exception e) {
            log.error("게시글 작성 중 오류 발생", e);
            model.addAttribute("error", "게시글 작성 중 오류가 발생했습니다.");
            return "post-write";
        }
    }

    // ============================================
    // 게시글 상세 조회
    // ============================================

    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model) {
        try {
            Post post = postService.getPostById(id);
            model.addAttribute("post", post);

            // 좋아요/싫어요 수 조회 (Service를 통해)
            long likeCount = postService.getLikeCount(id);
            long hateCount = postService.getHateCount(id);
            model.addAttribute("likeCount", likeCount);
            model.addAttribute("hateCount", hateCount);

            // 로그인 정보 및 권한 확인
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("username", username);

            boolean isAuthor = username != null && post.isAuthor(username);
            model.addAttribute("isAuthor", isAuthor);

            Long userId = (Long) session.getAttribute("userId");
            String userChoice = postService.getUserChoice(userId, id);
            model.addAttribute("userChoice", userChoice);

            // 댓글 목록 조회 (Service를 통해)
            List<Comment> comments = postService.getCommentsByPostId(id);
            model.addAttribute("comments", comments);
            return "post-detail";

        } catch (Exception e) {
            log.warn("게시글 조회 실패: {}", e.getMessage());
            return "redirect:/posts";
        }
    }

    // ============================================
    // 좋아요/싫어요 API
    // ============================================

    @PostMapping("/api/{postId}/like-hate")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Long>> likeHate(@PathVariable Long postId,
            @RequestParam RecommendationType type,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        lhService.toggleLikeHate(userId, postId, type);

        long likeCount = postService.getLikeCount(postId);
        long hateCount = postService.getHateCount(postId);
        return ResponseEntity.ok(java.util.Map.of("likeCount", likeCount, "hateCount", hateCount));
    }

    // ============================================
    // 게시글 삭제
    // ============================================

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long id,
            HttpSession session) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        boolean success = postService.deletePost(id, username);
        if (!success) {
            log.warn("게시글 삭제 실패: 권한 없음 또는 존재하지 않는 게시글");
        }

        return "redirect:/posts";
    }

    // ============================================
    // 게시글 수정 폼
    // ============================================

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            log.warn("비로그인 사용자가 수정 시도");
            return "redirect:/auth/login";
        }

        try {
            Post post = postService.getPostById(id);

            if (!post.isAuthor(username)) {
                log.warn("권한 없는 사용자가 수정 시도: {}", username);
                return "redirect:/posts/" + id;
            }

            model.addAttribute("post", post);
            model.addAttribute("username", username);

            log.info("게시글 수정 폼 접근: {}", id);
            return "post-edit";

        } catch (Exception e) {
            log.warn("게시글 조회 실패: {}", e.getMessage());
            return "redirect:/posts";
        }
    }

    // ============================================
    // 게시글 수정 처리
    // ============================================

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpSession session,
            Model model) {
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        // 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            model.addAttribute("error", "제목을 입력해주세요.");
            try {
                Post post = postService.getPostById(id);
                model.addAttribute("post", post);
                return "post-edit";
            } catch (Exception e) {
                return "redirect:/posts";
            }
        }

        if (content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "내용을 입력해주세요.");
            try {
                Post post = postService.getPostById(id);
                model.addAttribute("post", post);
                return "post-edit";
            } catch (Exception e) {
                return "redirect:/posts";
            }
        }

        boolean success = postService.updatePost(id, title, content, username);

        if (!success) {
            log.warn("게시글 수정 실패: 권한 없음 또는 존재하지 않는 게시글");
            model.addAttribute("error", "게시글 수정에 실패했습니다.");
            try {
                Post post = postService.getPostById(id);
                model.addAttribute("post", post);
                return "post-edit";
            } catch (Exception e) {
                return "redirect:/posts";
            }
        }

        log.info("게시글 수정 완료: {}", id);
        return "redirect:/posts/" + id;
    }

    // ============================================
    // 실시간 업데이트: 새 게시글 조회 API
    // ============================================

    @GetMapping("/api/new")
    @ResponseBody
    public List<Post> getNewPosts(@RequestParam String since) {
        try {
            LocalDateTime sinceTime = LocalDateTime.parse(since);
            return postService.getNewPostsSince(sinceTime);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
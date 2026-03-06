package com.example.demo.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Lh;
import com.example.demo.entity.Post;
import com.example.demo.entity.PostImage;
import com.example.demo.entity.RecommendationType;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CommentService;
import com.example.demo.service.LhService;
import com.example.demo.service.PostService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 📌 게시글(Post) 관련 컨트롤러
 * 
 * 학습 포인트:
 * 1. @RequestMapping을 활용한 URL 그룹화
 * 2. RESTful URL 설계 패턴
 * 3. Service 계층을 통한 비즈니스 로직 분리
 * 4. @PathVariable을 활용한 동적 URL 매핑
 * 
 * URL 구조:
 * - GET /posts → 게시글 목록
 * - GET /posts/write → 글쓰기 폼
 * - POST /posts/write → 글쓰기 처리
 * - GET /posts/{id} → 게시글 상세 조회
 * - GET /posts/{id}/edit → 게시글 수정 폼
 * - POST /posts/{id}/edit → 게시글 수정 처리
 * - POST /posts/{id}/delete → 게시글 삭제
 * 
 * 💡 왜 Service를 사용할까?
 * - Controller: HTTP 요청/응답 처리만 담당
 * - Service: 비즈니스 로직 (권한 확인, 데이터 검증 등)
 * - Repository: DB 접근
 * → 각 계층의 역할을 명확히 분리 (관심사의 분리)
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/posts") // 이 컨트롤러의 모든 URL은 /posts로 시작
public class PostController {

    // 허용된 이미지 확장자 (보안)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp");

    private final LhRepository lhRepository;
    private final LhService lhService;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    // 1. 상세 페이지 조회 (여기서 카운트를 수행해서 HTML에 넘김)
    @GetMapping("/post/{postId}")
    public String getPostDetail(@PathVariable Long postId, Model model) {
        Post post = postService.getPostById(postId);

        // LH 테이블에서 실시간 검색
        long likeCount = lhRepository.countByPostIdAndType(postId, RecommendationType.L);
        long hateCount = lhRepository.countByPostIdAndType(postId, RecommendationType.H);

        model.addAttribute("post", post);
        model.addAttribute("likeCount", likeCount); // HTML의 ${likeCount}와 매칭
        model.addAttribute("hateCount", hateCount);
        return "post-detail";
    }

    // 2. 추천/비추천 버튼 클릭 처리 (API)
    @PostMapping("/api/{postId}/like-hate")
    @ResponseBody // RestController처럼 결과만 반환
    public ResponseEntity<String> likeHate(@PathVariable Long postId,
            @RequestParam RecommendationType type,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            // 2. body 없이 401 상태 코드만 반환
            return ResponseEntity.status(401).build();
        }

        // 3. 서비스 호출 (리턴값을 받지 않음)
        lhService.toggleLikeHate(userId, postId, type);

        // 4. 성공 응답 반환
        return ResponseEntity.ok().build();
    }

    /**
     * Service 계층 주입
     * 
     * 학습 포인트:
     * - @Autowired로 PostService를 자동 주입
     * - Controller는 Repository를 직접 사용하지 않음
     * - Service를 통해 비즈니스 로직을 처리
     */

    // ============================================
    // 게시글 목록
    // ============================================
    @Autowired
    private PostService postService;

    /**
     * 게시글 목록 조회
     * URL: /posts (GET)
     * 
     * 학습 포인트:
     * 1. @GetMapping (파라미터 없음) → /posts에 매핑
     * 2. 로그인 여부 확인 (선택사항)
     * 3. Service를 통한 데이터 조회
     * 4. List<Post>를 뷰에 전달
     * 
     * 💡 왜 @GetMapping만 사용?
     * - @RequestMapping("/posts") + @GetMapping
     * - 결과: GET /posts
     */
    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session, Model model) {
        String username = (String) session.getAttribute("loginUser");
        model.addAttribute("username", username);
        model.addAttribute("keyword", keyword);

        Page<Post> postsPage = postService.getPagedPosts(keyword, page);
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

    /**
     * 게시글 작성 폼 표시
     * URL: /posts/write (GET)
     * 
     * 학습 포인트:
     * - 로그인 확인 필수 (비로그인 시 리다이렉트)
     * - GET: 폼만 표시, POST: 실제 저장
     * - 같은 URL, 다른 HTTP 메서드 → 다른 처리
     */
    @GetMapping("/write")
    public String writeForm(HttpSession session, Model model) {
        // 로그인 확인
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            System.out.println("❌ 비로그인 사용자가 글쓰기 시도");
            return "redirect:/auth/login";
        }

        model.addAttribute("username", username);
        return "post-write"; // templates/post-write.html
    }

    // ============================================
    // 게시글 작성 처리
    // ============================================

    /**
     * 게시글 작성 처리
     * URL: /posts/write (POST)
     * 
     * 학습 포인트:
     * 1. @RequestParam: form의 input name과 매핑
     * 2. 입력값 검증 (제목, 내용 필수)
     * 3. Service를 통한 게시글 생성
     * 4. redirect: 작성 후 상세 페이지로 이동
     * 
     * 💡 왜 redirect를 사용할까?
     * - forward: URL은 그대로, 뷰만 변경 (새로고침 시 중복 등록)
     * - redirect: 새로운 URL로 이동 (새로고침 해도 안전)
     */
    @PostMapping("/write")
    public String write(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("imageFile") List<MultipartFile> imageFiles,
            HttpSession session,
            Model model) throws IOException {
        // 로그인 확인
        String username = (String) session.getAttribute("loginUser");

        if (username == null) {
            return "redirect:/login";
        }

        // 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            model.addAttribute("error", "제목을 입력해주세요.");
            return "post-write";
        }

        if (content == null || content.trim().isEmpty()) {
            model.addAttribute("error", "내용을 입력해주세요.");
            return "post-write";
        }

        // 게시글 작성
        try {
            // 이미지가 있을 때 처리할 변수들
            List<String> fileNames = new ArrayList<>();
            List<String> filePaths = new ArrayList<>();

            String uploadDir = "C:/starlog/upload/";
            File folder = new File(uploadDir);
            if (!folder.exists())
                folder.mkdirs();

            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

                    // 파일 확장자 검증
                    if (!ALLOWED_EXTENSIONS.contains(extension)) {
                        model.addAttribute("error", "허용되지 않는 파일 형식입니다. (jpg, png, gif, webp만 가능)");
                        return "post-write";
                    }

                    String uuid = UUID.randomUUID().toString();
                    String fileName = uuid + "." + extension;

                    file.transferTo(new File(uploadDir + fileName));

                    fileNames.add(fileName);
                    filePaths.add("/upload/" + fileName);

                    System.out.println("✅ 파일 저장 성공: " + fileName);
                }
            }
            Post post = postService.createPost(title, content, username, fileNames, filePaths);
            System.out.println("✅ 게시글 작성 완료: " + post.getId());
            return "redirect:/posts/" + post.getId();

        } catch (Exception e) {
            e.printStackTrace(); // 에러 발생 시 콘솔에 상세 내용 출력
            model.addAttribute("error", "게시글 작성 중 오류가 발생했습니다.");
            return "post-write";
        }
    }

    // ============================================
    // 게시글 상세 조회
    // ============================================

    /**
     * 게시글 상세 조회
     * URL: /posts/{id} (GET)
     * 예) /posts/1, /posts/42 등
     * 
     * 학습 포인트:
     * 1. @PathVariable: URL의 {id} 부분을 변수로 받음
     * 2. 동적 URL 매핑 (RESTful 설계의 핵심)
     * 3. 예외 처리 (게시글이 없는 경우)
     * 4. 작성자 확인 로직 (수정/삭제 버튼 표시용)
     * 
     * 💡 @PathVariable vs @RequestParam 차이:
     * - @PathVariable: /posts/1 (URL 경로의 일부)
     * - @RequestParam: /posts?id=1 (쿼리 파라미터)
     */
    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") Long id, // URL의 {id}를 Long 타입으로 받음
            HttpSession session,
            Model model) {
        try {
            // 1. 게시글 정보 조회 (postService 내에 getPostById 메서드 사용)
            Post post = postService.getPostById(id);
            model.addAttribute("post", post);

            // 2. LH 테이블에서 해당 게시물의 'L' 개수와 'H' 개수를 각각 검색 (Count)
            long likeCount = lhRepository.countByPostIdAndType(id, RecommendationType.L);
            long hateCount = lhRepository.countByPostIdAndType(id, RecommendationType.H);

            model.addAttribute("likeCount", likeCount);
            model.addAttribute("hateCount", hateCount);

            // 3. 로그인 정보 및 권한 확인
            String username = (String) session.getAttribute("loginUser");
            model.addAttribute("username", username);

            boolean isAuthor = username != null && post.isAuthor(username);
            model.addAttribute("isAuthor", isAuthor);

            Long userId = (Long) session.getAttribute("userId");
            String userChoice = ""; // 기본값 (아무것도 안 누름)

            if (userId != null) {
                // DB에서 해당 유저가 이 게시글에 남긴 기록이 있는지 조회
                Optional<Lh> myLh = lhRepository.findByUserIdAndPostId(userId, id);
                if (myLh.isPresent()) {
                    userChoice = myLh.get().getType().toString(); // "L" 또는 "H"
                }
            }
            model.addAttribute("userChoice", userChoice); // HTML로 "L", "H" 혹은 "" 전달

            List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(id);
            model.addAttribute("comments", comments);
            return "post-detail";

        } catch (Exception e) {
            System.out.println("❌ 게시글 조회 실패: " + e.getMessage());
            return "redirect:/posts";
        }
    }
    // //댓글달기
    // @PostMapping("/{id}/comments")
    // public String addComment(@PathVariable("id") Long id,
    // @RequestParam String content,
    // HttpSession session) {
    // Long userId = (Long) session.getAttribute("userId");
    // if (userId == null) return "redirect:/auth/login";

    // commentService.createComment(id, userId, content);
    // return "redirect:/posts/" + id; // 작성 후 상세페이지로 리다이렉트
    // }
    // ============================================
    // 게시글 삭제
    // ============================================

    /**
     * 게시글 삭제
     * URL: /posts/{id}/delete (POST)
     * 예) /posts/1/delete
     * 
     * 학습 포인트:
     * 1. 삭제는 반드시 POST 사용 (보안)
     * 2. @PathVariable로 삭제할 게시글 ID 받기
     * 3. Service에서 권한 확인 (작성자만 삭제 가능)
     * 4. 삭제 후 목록으로 리다이렉트
     * 
     * 💡 왜 GET /posts/{id}/delete는 위험할까?
     * - 브라우저 캐시, 검색엔진 크롤러 등이 URL 접근 시 삭제됨
     * - <img src="/posts/1/delete"> 같은 공격 가능
     * - 반드시 POST, PUT, DELETE 같은 메서드 사용!
     */
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable("id") Long id,
            HttpSession session) {
        // 로그인 확인
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        // 삭제 처리 (Service에서 권한 확인)
        boolean success = postService.deletePost(id, username);

        if (!success) {
            System.out.println("❌ 게시글 삭제 실패: 권한 없음 또는 존재하지 않는 게시글");
        }

        return "redirect:/posts"; // 삭제 후 목록으로
    }

    // ============================================
    // 게시글 수정 폼
    // ============================================

    /**
     * 게시글 수정 폼 표시
     * URL: /posts/{id}/edit (GET)
     * 예) /posts/12/edit
     * 
     * 학습 포인트:
     * 1. 수정 폼에는 기존 데이터를 미리 채워야 함
     * 2. 작성자만 수정 가능 (권한 확인)
     * 3. Model에 post 객체를 담아서 뷰에 전달
     * 4. 폼에서 th:value="${post.title}" 형태로 사용
     * 
     * 💡 수정 vs 작성의 차이:
     * - 작성: 빈 폼 제공
     * - 수정: 기존 데이터가 채워진 폼 제공
     */
    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable("id") Long id,
            HttpSession session,
            Model model) {
        // 1. 로그인 확인
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            System.out.println("❌ 비로그인 사용자가 수정 시도");
            return "redirect:/auth/login";
        }

        try {
            // 2. 게시글 조회
            Post post = postService.getPostById(id);

            // 3. 작성자 확인 (중요!)
            if (!post.isAuthor(username)) {
                System.out.println("❌ 권한 없는 사용자가 수정 시도: " + username);
                return "redirect:/posts/" + id; // 상세 페이지로 리다이렉트
            }

            // 4. 폼에 데이터 전달
            model.addAttribute("post", post);
            model.addAttribute("username", username);

            System.out.println("게시글 수정 폼 접근: " + id);
            return "post-edit"; // templates/post-edit.html

        } catch (Exception e) {
            System.out.println("❌ 게시글 조회 실패: " + e.getMessage());
            return "redirect:/posts";
        }
    }

    // ============================================
    // 게시글 수정 처리
    // ============================================

    /**
     * 게시글 수정 처리
     * URL: /posts/{id}/edit (POST)
     * 
     * 학습 포인트:
     * 1. @PathVariable로 수정할 게시글 ID 받기
     * 2. @RequestParam로 수정된 내용 받기
     * 3. Service에서 권한 확인 및 수정 처리
     * 4. 성공 시 상세 페이지로, 실패 시 다시 수정 폼으로
     * 
     * 💡 RESTful하게 하려면?
     * - PUT /posts/{id} 를 사용하는 게 이상적
     * - 하지만 HTML form은 GET/POST만 지원
     * - 실무: POST /posts/{id}/edit 또는 HiddenHttpMethodFilter 사용
     */
    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable("id") Long id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            HttpSession session,
            Model model) {
        // 1. 로그인 확인
        String username = (String) session.getAttribute("loginUser");
        if (username == null) {
            return "redirect:/auth/login";
        }

        // 2. 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            model.addAttribute("error", "제목을 입력해주세요.");
            // 수정 실패 시 다시 폼으로 (기존 데이터 유지)
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

        // 3.수정 처리 (Service에서 권한 확인 포함)
        boolean success = postService.updatePost(id, title, content, username);

        if (!success) {
            System.out.println("게시글 수정 실패: 권한 없음 또는 존재하지 않는 게시글");
            model.addAttribute("error", "게시글 수정에 실패했습니다.");
            try {
                Post post = postService.getPostById(id);
                model.addAttribute("post", post);
                return "post-edit";
            } catch (Exception e) {
                return "redirect:/posts";
            }
        }

        // 4. 성공 시 상세 페이지로 리다이렉트
        System.out.println("게시글 수정 완료: " + id);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/save")
    public String savePost(@ModelAttribute Post post,
            @RequestParam("imageFile") List<MultipartFile> imageFiles,
            HttpSession session) throws IOException {

        Long userId = (Long) session.getAttribute("userId");
        User user = userRepository.findById(userId).orElseThrow();
        post.setUser(user);

        String uploadDir = "C:/starlog/upload/";
        File folder = new File(uploadDir);

        // 폴더가 없으면 생성 (이 코드가 실행되는지 로그를 찍어보세요)
        if (!folder.exists()) {
            folder.mkdirs();
            System.out.println("폴더 생성 완료: " + uploadDir);
        }

        // 이미지 파일이 비어있지 않은 경우에만 처리
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

                    // 파일 확장자 검증
                    if (!ALLOWED_EXTENSIONS.contains(extension)) {
                        continue; // 허용되지 않는 확장자는 건너뜀
                    }

                    String uuid = UUID.randomUUID().toString();
                    String savedName = uuid + "." + extension;

                    // 1. 파일 시스템에 저장
                    File saveFile = new File(uploadDir + savedName);
                    file.transferTo(saveFile);

                    // 2. PostImage 객체 생성 및 Post와 연결 (setFilePath 대신 이 방식을 씁니다)
                    PostImage postImage = new PostImage(savedName, "/upload/" + savedName, post);
                    post.getImages().add(postImage); // Post 엔티티 내부의 List에 추가
                }
            }
        }

        postRepository.save(post);
        return "redirect:/posts";
    }

    // ============================================
    // 실시간 업데이트: 새 게시글 조회 API
    // ============================================
    /**
     * 실시간 폴링을 위한 새 게시글 조회 API
     * URL: /posts/api/new (GET)
     * 
     * @param since - 마지막 확인 시간 (ISO 8601 형식)
     * @return JSON 형식의 새 게시글 목록
     */
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
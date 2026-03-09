package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Comment;
import com.example.demo.entity.Lh;
import com.example.demo.entity.Post;
import com.example.demo.entity.PostImage;
import com.example.demo.entity.RecommendationType;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.HangulUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LhRepository lhRepository;
    private final CommentRepository commentRepository;

    // ============================================
    // 게시글 작성
    // ============================================

    public Post createPost(String title, String content, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", username));

        // XSS 방지: HTML 태그 제거
        title = Jsoup.clean(title, Safelist.none());
        content = Jsoup.clean(content, Safelist.none());

        Post post = new Post(title, content, user);
        Post savedPost = postRepository.save(post);

        log.info("게시글 작성 완료: {}", savedPost);
        return savedPost;
    }

    public Post createPost(String title, String content, String username, List<String> fileNames,
            List<String> filePaths) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", username));

        // XSS 방지: HTML 태그 제거
        title = Jsoup.clean(title, Safelist.none());
        content = Jsoup.clean(content, Safelist.none());

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());

        // 여러 이미지 정보를 PostImage 객체로 만들어 Post에 추가
        if (filePaths != null) {
            for (int i = 0; i < filePaths.size(); i++) {
                PostImage image = new PostImage(fileNames.get(i), filePaths.get(i), post);
                post.getImages().add(image);
            }
        }

        return postRepository.save(post);
    }

    // ============================================
    // 게시글 목록 조회 (전체)
    // ============================================

    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    // ============================================
    // 게시글 상세 조회 (조회수 증가)
    // ============================================

    public Post getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", id));

        // 조회수 증가
        post.incrementViewCount();
        postRepository.save(post);

        log.info("게시글 조회: {} (조회수: {})", post.getTitle(), post.getViewCount());
        return post;
    }

    /**
     * 조회수를 증가시키지 않고 게시글을 조회합니다.
     * (세션에서 이미 조회한 게시글에 대한 재조회 시 사용)
     */
    @Transactional(readOnly = true)
    public Post getPostByIdReadOnly(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글", id));
    }

    // ============================================
    // 내가 쓴 글 조회
    // ============================================

    @Transactional(readOnly = true)
    public List<Post> getMyPosts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", username));
        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<Post> getPopularPosts(int page) {
        Pageable pageable = PageRequest.of(page, 5);
        return postRepository.findAllByOrderByLikeCountDescCreatedAtAsc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Post> getPagedPosts(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        if (keyword != null && !keyword.isEmpty()) {
            // 기본 검색 (DB 수준)
            return postRepository.findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
                    keyword, keyword, pageable);
        }
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 향상된 한글 검색 (초성, 자모 분리, 영타 지원)
     * 모든 게시글을 가져와서 필터링하므로, 데이터가 많으면 성능 저하 가능
     * 게시글이 적을 때 사용 권장
     */
    @Transactional(readOnly = true)
    public Page<Post> getPagedPostsEnhanced(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        
        if (keyword == null || keyword.isEmpty()) {
            return postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // 메모리에서 검색 (초성, 자모, 영타 등 고급 검색)
        List<Post> allPosts = postRepository.findAllByOrderByCreatedAtDesc();
        List<Post> filteredPosts = allPosts.stream()
                .filter(post -> 
                    HangulUtils.enhancedMatch(post.getTitle(), keyword) ||
                    HangulUtils.enhancedMatch(post.getContent(), keyword) ||
                    HangulUtils.enhancedMatch(post.getUser().getUsername(), keyword)
                )
                .sorted((p1, p2) -> {
                    // 검색 점수로 정렬 (높을수록 먼저)
                    int score1 = Math.max(
                        HangulUtils.calculateSearchScore(p1.getTitle(), keyword),
                        HangulUtils.calculateSearchScore(p1.getContent(), keyword)
                    );
                    int score2 = Math.max(
                        HangulUtils.calculateSearchScore(p2.getTitle(), keyword),
                        HangulUtils.calculateSearchScore(p2.getContent(), keyword)
                    );
                    if (score1 != score2) {
                        return Integer.compare(score2, score1);
                    }
                    // 점수가 같으면 최신순
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .toList();

        // 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredPosts.size());
        
        List<Post> pageContent = filteredPosts.subList(start, end);
        
        log.info("향상된 검색: '{}' -> {} 건 찾음", keyword, filteredPosts.size());
        
        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, filteredPosts.size()
        );
    }

    // ============================================
    // 게시글 수정
    // ============================================

    public boolean updatePost(Long id, String title, String content, String username) {
        Optional<Post> postOptional = postRepository.findById(id);
        if (postOptional.isEmpty()) {
            log.warn("게시글이 존재하지 않습니다. (ID: {})", id);
            return false;
        }

        Post post = postOptional.get();

        if (!post.isAuthor(username)) {
            log.warn("수정 권한 없음 (작성자: {}, 요청자: {})", post.getUser().getUsername(), username);
            return false;
        }

        // XSS 방지
        title = Jsoup.clean(title, Safelist.none());
        content = Jsoup.clean(content, Safelist.none());

        post.setTitle(title);
        post.setContent(content);
        postRepository.save(post);

        log.info("게시글 수정 완료: {}", post.getTitle());
        return true;
    }

    // ============================================
    // 게시글 삭제
    // ============================================

    public boolean deletePost(Long id, String username) {
        Optional<Post> postOptional = postRepository.findById(id);
        if (postOptional.isEmpty()) {
            log.warn("삭제 대상 게시글이 존재하지 않습니다. (ID: {})", id);
            return false;
        }

        Post post = postOptional.get();

        if (!post.isAuthor(username)) {
            log.warn("삭제 권한 없음");
            return false;
        }

        postRepository.delete(post);
        log.info("게시글 삭제 완료: {}", post.getTitle());
        return true;
    }

    // ============================================
    // 게시글 검색
    // ============================================

    @Transactional(readOnly = true)
    public List<Post> searchPosts(String keyword) {
        return postRepository.findByTitleContainingOrContentContainingOrderByCreatedAtDesc(keyword, keyword);
    }

    /**
     * 향상된 검색 (초성 검색, 자모 분리, 영타 변환 지원)
     * 예제:
     * - "안녕" 검색 시: 일반 텍스트 모든 게시글
     * - "ㄴㄴ" 검색 시: 초성이 ㄴㄴ인 모든 게시글 (예: 안녕, 아나운서, 알림 등)
     * - "안" 검색 시: 자모가 분리된 텍스트 포함
     * - "dkssud" 검색 시: 영타를 한글로 변환하여 검색 ("안녕" 찾음)
     */
    @Transactional(readOnly = true)
    public List<Post> searchPostsEnhanced(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return List.of();
        }

        List<Post> allPosts = postRepository.findAllByOrderByCreatedAtDesc();
        
        return allPosts.stream()
                .filter(post -> 
                    HangulUtils.enhancedMatch(post.getTitle(), keyword) ||
                    HangulUtils.enhancedMatch(post.getContent(), keyword) ||
                    HangulUtils.enhancedMatch(post.getUser().getUsername(), keyword)
                )
                .sorted((p1, p2) -> {
                    int score1 = Math.max(
                        HangulUtils.calculateSearchScore(p1.getTitle(), keyword),
                        HangulUtils.calculateSearchScore(p1.getContent(), keyword)
                    );
                    int score2 = Math.max(
                        HangulUtils.calculateSearchScore(p2.getTitle(), keyword),
                        HangulUtils.calculateSearchScore(p2.getContent(), keyword)
                    );
                    if (score1 != score2) {
                        return Integer.compare(score2, score1);
                    }
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .toList();
    }

    // ============================================
    // 통계
    // ============================================

    @Transactional(readOnly = true)
    public long getTotalPostCount() {
        return postRepository.count();
    }

    @Transactional(readOnly = true)
    public long getUserPostCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", username));
        return postRepository.countByUser(user);
    }

    // ============================================
    // 좋아요/싫어요 관련 (Controller에서 이동)
    // ============================================

    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return lhRepository.countByPostIdAndType(postId, RecommendationType.L);
    }

    @Transactional(readOnly = true)
    public long getHateCount(Long postId) {
        return lhRepository.countByPostIdAndType(postId, RecommendationType.H);
    }

    @Transactional(readOnly = true)
    public String getUserChoice(Long userId, Long postId) {
        if (userId == null) {
            return "";
        }
        Optional<Lh> myLh = lhRepository.findByUserIdAndPostId(userId, postId);
        return myLh.map(lh -> lh.getType().toString()).orElse("");
    }

    // ============================================
    // 댓글 관련 (Controller에서 이동)
    // ============================================

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    // ============================================
    // 실시간 업데이트: 특정 시간 이후 새 게시글 조회
    // ============================================

    @Transactional(readOnly = true)
    public List<Post> getNewPostsSince(LocalDateTime since) {
        return postRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
    }
}

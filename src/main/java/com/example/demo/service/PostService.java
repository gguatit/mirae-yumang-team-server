package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.entity.RecommendationType;
import com.example.demo.entity.User;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;
    
    private final LhRepository lhRepository;

    // ============================================
    // 게시글 작성
    // ============================================

    public Post createPost(String title, String content, String username) {
        // 1. 작성자 확인
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. 게시글 생성
        Post post = new Post(title, content, user);

        // 3. 저장
        Post savedPost = postRepository.save(post);

        System.out.println("게시글 작성 완료: " + savedPost);
        return savedPost;
    }

    // ============================================
    // 게시글 목록 조회 (전체)
    // ============================================

    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    // ============================================
    // 게시글 상세 조회 (조회수 증가)
    // ============================================

    public Post getPostById(Long id) {
        Optional<Post> postOptional = postRepository.findById(id);

        if (postOptional.isEmpty()) {
            throw new RuntimeException("게시글을 찾을 수 없습니다.");
        }

        Post post = postOptional.get();

        // 조회수 증가
        post.incrementViewCount();
        postRepository.save(post);

        System.out.println("게시글 조회: " + post.getTitle() + " (조회수: " + post.getViewCount() + ")");

        return post;
    }

    // ============================================
    // 내가 쓴 글 조회
    // ============================================

    public List<Post> getMyPosts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }
    public Page<Post> getPopularPosts(int page) {
        Pageable pageable = PageRequest.of(page, 5);
        return postRepository.findAllByOrderByLikeCountDescCreatedAtAsc(pageable);
    }

    // ============================================
    // 게시글 수정
    // ============================================

    public boolean updatePost(Long id, String title, String content, String username) {
        // 1. 게시글 존재 확인
        Optional<Post> postOptional = postRepository.findById(id);
        if (postOptional.isEmpty()) {
            System.out.println("❌ 게시글이 존재하지 않습니다.");
            return false;
        }

        Post post = postOptional.get();

        // 2. 작성자 확인 (중요!)
        if (!post.isAuthor(username)) {
            System.out.println("❌ 수정 권한이 없습니다. (작성자: " + post.getUser().getUsername() + ", 요청자: " + username + ")");
            return false;
        }

        // 3. 수정 (Dirty Checking)
        post.setTitle(title);
        post.setContent(content);
        postRepository.save(post);

        System.out.println("게시글 수정 완료: " + post.getTitle());
        return true;
    }

    // ============================================
    // 게시글 삭제
    // ============================================

    public boolean deletePost(Long id, String username) {
        // 1. 게시글 존재 확인
        Optional<Post> postOptional = postRepository.findById(id);
        if (postOptional.isEmpty()) {
            System.out.println("❌ 게시글이 존재하지 않습니다.");
            return false;
        }

        Post post = postOptional.get();

        // 2. 작성자 확인 (중요!)
        if (!post.isAuthor(username)) {
            System.out.println("❌ 삭제 권한이 없습니다.");
            return false;
        }

        // 3. 삭제
        postRepository.delete(post);

        System.out.println("게시글 삭제 완료: " + post.getTitle());
        return true;
    }

    // ============================================
    // 게시글 검색
    // ============================================

    public List<Post> searchPosts(String keyword) {
        return postRepository.findByTitleContainingOrContentContainingOrderByCreatedAtDesc(keyword, keyword);
    }

    // ============================================
    // 통계
    // ============================================

    public long getTotalPostCount() {
        return postRepository.count();
    }

    public long getUserPostCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return postRepository.countByUser(user);
    }}
    
package com.example.demo.service;

import com.example.demo.dto.CommentResponseDto;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        // XSS 방지: HTML 태그 제거
        content = Jsoup.clean(content, Safelist.none());

        Comment comment = Comment.builder()
                .content(content)
                .post(post)
                .user(user)
                .build();

        commentRepository.save(comment);
        log.info("댓글 작성 완료: postId={}, userId={}", postId, userId);
    }

    @Transactional
    public CommentResponseDto saveComment(Long postId, String username, String content, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findByUsername(username).orElseThrow();

        // XSS 방지: HTML 태그 제거
        content = Jsoup.clean(content, Safelist.none());

        Comment comment = new Comment(content, post, user);

        // 대댓글 로직: 부모가 있다면 연결
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId).orElseThrow();
            comment.setParent(parent);
        }

        Comment savedComment = commentRepository.save(comment);
        log.info("댓글 저장: postId={}, username={}, parentId={}", postId, username, parentId);

        return new CommentResponseDto(
                savedComment.getId(),
                savedComment.getContent(),
                savedComment.getUser().getUsername(),
                parentId,
                "방금 전"
        );
    }
}
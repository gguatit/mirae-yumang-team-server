package com.example.demo.service;

import com.example.demo.dto.CommentResponseDto;
import com.example.demo.entity.Comment;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    }

    @Transactional
    public CommentResponseDto saveComment(Long postId, String username, String content, Long parentId) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findByUsername(username).orElseThrow();

        // XSS 방지: HTML 태그 제거
        content = Jsoup.clean(content, Safelist.none());

        Comment comment = new Comment(content, post, user);

        // 💡 대댓글 로직: 부모가 있다면 연결해줌
        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId).orElseThrow();
            comment.setParent(parent); // 👈 여기서도 setParent가 필요합니다!
        }

        Comment savedComment = commentRepository.save(comment);

        // 컨트롤러에 전달할 DTO 반환
        return new CommentResponseDto(
                savedComment.getId(),
                savedComment.getContent(),
                savedComment.getUser().getUsername(),
                parentId,
                "방금 전" // 혹은 포맷팅된 시간
        );
    }
}
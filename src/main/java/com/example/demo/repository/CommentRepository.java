package com.example.demo.repository;

import com.example.demo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 게시글 ID로 댓글 목록을 작성 시간 순으로 가져오기
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 회원 탈퇴 시 해당 유저의 모든 댓글 삭제
    void deleteByUser(com.example.demo.entity.User user);
}
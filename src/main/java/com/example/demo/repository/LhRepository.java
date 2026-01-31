package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Lh;
import com.example.demo.entity.Post;
import com.example.demo.entity.RecommendationType;
import com.example.demo.entity.User;

public interface LhRepository extends JpaRepository<Lh, Long> {
    // 특정 유저가 특정 게시물에 남긴 기록 찾기
    Optional<Lh> findByUserAndPost(User user, Post post);
    
    // 게시물당 좋아요/싫어요 개수 세기
    long countByPostAndType(Post post, RecommendationType type);

    // 특정 게시물에 대해 특정 타입(L 또는 H)의 개수를 세는 쿼리
    long countByPostIdAndType(Long postId, RecommendationType h);

    // 특정 유저가 특정 게시물에 남긴 기록 조회 (토글용)
    Optional<Lh> findByUserIdAndPostId(Long userId, Long postId);
}

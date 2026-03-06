package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;

/**
 * Post 관련 DB 접근 계층 (Repository)
 *
 * 학습 포인트:
 * - Spring Data JPA의 쿼리 메서드(Query Method)를 활용하면
 *   메서드 이름만으로도 SQL이 자동 생성됩니다.
 * - 메서드 이름에 정렬(OrderBy), 조건(Containing), 관계(ByUser) 등을
 *   결합하여 다양한 쿼리를 구현할 수 있습니다.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 1) 전체 게시글 조회 (최신순)
    //    SQL: SELECT * FROM posts ORDER BY created_at DESC
    List<Post> findAllByOrderByCreatedAtDesc();

    // 2) 특정 사용자의 게시글 조회 (내 글 보기 등)
    List<Post> findByUserOrderByCreatedAtDesc(User user);

    // 3) 제목으로 검색 (부분 일치)
    List<Post> findByTitleContainingOrderByCreatedAtDesc(String keyword);

    // 4) 제목 또는 내용으로 검색 (키워드 검색용)
    List<Post> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
            String titleKeyword, String contentKeyword);

    // 5) 특정 사용자가 작성한 게시글 개수 (통계용)
    long countByUser(User user);

    // 6) 전체 목록 페이지네이션
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 7) 검색 + 페이지네이션
    Page<Post> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(
            String titleKeyword, String contentKeyword, Pageable pageable);

    // 인기 게시글 (추천순)
    Page<Post> findAllByOrderByLikeCountDescCreatedAtAsc(Pageable pageable);

    // 8) 특정 시간 이후 생성된 게시글 조회 (실시간 업데이트용)
    List<Post> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

}
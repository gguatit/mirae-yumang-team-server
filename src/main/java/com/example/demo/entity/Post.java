package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "posts")
@NoArgsConstructor // JPA 필수 (기본 생성자)
@AllArgsConstructor // @Builder와 함께 사용
@Builder // 선택적 (테스트, 복잡한 객체 생성 시 유용)
@Getter // 필수
@Setter // 엔티티에서는 신중하게 사용
// @ToString은 직접 구현 권장 (LAZY 로딩 문제 때문)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // ✨ User와의 관계 설정 (핵심!)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"posts", "password", "createdAt", "updatedAt"})
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(columnDefinition = "integer default 0")
    private int likeCount = 0; // 초기값도 0으로 설정

    @Column(columnDefinition = "integer default 0")
    private int hateCount = 0;
    //이미지 추가
    @Column
    private String fileName; // 실제 저장된 파일 이름 (UUID_원본이름.jpg 형태)

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"post"})
    private List<PostImage> images = new ArrayList<>();

    // 편의 생성자
    public Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.viewCount = 0;
        this.likeCount = 0;
        this.hateCount = 0;
    }

    // ✨ 생성 시 자동으로 현재 시간 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
    }

    // ✨ 수정 시 자동으로 현재 시간 설정
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================
    // 비즈니스 로직 메서드
    // ============================================
    // 비즈니스 로직: 숫자를 증감시키는 메서드
    public void updateLikeCount(int amount) {
        this.likeCount += amount;
    }

    public void updateHateCount(int amount) {
        this.hateCount += amount;
    }
    /**
     * 조회수 증가 메서드
     * 
     * 학습 포인트:
     * - 엔티티 내부에서 자신의 상태를 변경하는 메서드
     * - 이런 방식을 "도메인 모델 패턴"이라고 함
     * - 데이터와 로직을 함께 관리 (객체지향적)
     * 
     * 💡 주의: 이건 단순 setter가 아니라 비즈니스 로직!
     * - viewCount++를 캡슐화
     * - Lombok @Setter로는 구현 불가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 작성자 확인 메서드
     * 
     * 학습 포인트:
     * - 권한 확인 로직을 엔티티에 캡슐화
     * - Controller나 Service에서 쉽게 사용 가능
     * - 코드 중복 방지
     * 
     * 💡 주의: 이건 단순 getter가 아니라 비즈니스 로직!
     * - username 비교 로직 포함
     * - Lombok @Getter로는 구현 불가
     */
    public boolean isAuthor(String username) {
        return this.user != null && this.user.getUsername().equals(username);
    }

    /**
     * 작성자 이름 조회 (편의 메서드)
     * 
     * 학습 포인트:
     * - LAZY 로딩 문제 해결을 위한 편의 메서드
     * - 템플릿에서 ${post.author}로 간단하게 접근 가능
     * - null-safe: user가 null이어도 "알 수 없음" 반환
     * 
     * 💡 주의: 이건 단순 getter가 아니라 커스텀 getter!
     * - user.username을 안전하게 조회
     * - Lombok이 만드는 getUser()와는 다른 목적
     * - post.user.username은 LAZY 로딩 에러 가능
     * - getAuthor()는 트랜잭션 내에서 안전하게 로딩
     */
    public String getAuthor() {
        return this.user != null ? this.user.getUsername() : "알 수 없음";
    }

    /**
     * toString 커스텀 구현
     * 
     * 학습 포인트:
     * - @ToString 대신 직접 구현
     * - LAZY 로딩 에러 방지
     * - 순환 참조 방지 (User ↔ Post)
     * - 필요한 정보만 선택적으로 출력
     */
    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + getAuthor() + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
    // 🔥 추가: CascadeType.ALL과 orphanRemoval을 설정
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    // 초기화를 꼭 해주어야 NullPointerException이 발생하지 않습니다.
    private List<Lh> likesHates = new ArrayList<>();

    // 2. 댓글 데이터와의 관계 (이 부분이 없거나 설정이 부족할 확률이 높습니다)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
}
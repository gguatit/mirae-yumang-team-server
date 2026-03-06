package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // User와의 관계 설정
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
    private int likeCount = 0;

    @Column(columnDefinition = "integer default 0")
    private int hateCount = 0;

    // 이미지
    @Column
    private String fileName;

    // ============================================
    // 연관관계 필드 (일괄 관리)
    // ============================================

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"post"})
    private List<PostImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lh> likesHates = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // ============================================
    // 생성자
    // ============================================

    public Post(String title, String content, User user) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.viewCount = 0;
        this.likeCount = 0;
        this.hateCount = 0;
    }

    // ============================================
    // JPA 콜백
    // ============================================

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ============================================
    // 비즈니스 로직 메서드
    // ============================================

    public void updateLikeCount(int amount) {
        this.likeCount += amount;
    }

    public void updateHateCount(int amount) {
        this.hateCount += amount;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public boolean isAuthor(String username) {
        return this.user != null && this.user.getUsername().equals(username);
    }

    public String getAuthor() {
        return this.user != null ? this.user.getUsername() : "알 수 없음";
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + getAuthor() + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
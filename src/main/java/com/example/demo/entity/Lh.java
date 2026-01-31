package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키를 따로 두는 것이 관리가 편합니다.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POST_ID")
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", length = 1)
    private RecommendationType type; // 'L' 또는 'H'를 담는 Enum

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public Lh(User user, Post post, RecommendationType type) {
        this.user = user;
        this.post = post;
        this.type = type;
    }

    public void changeType(RecommendationType type) {
        this.type = type;
    }
    
}

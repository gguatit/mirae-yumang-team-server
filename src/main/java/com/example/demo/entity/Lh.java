package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "LH")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "POST_ID", nullable = false)
    private Long postId;

    /**
     * L : 좋아요
     * H : 싫어요
     */
    @Column(name = "TYPE", nullable = false, length = 1)
    private String type;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

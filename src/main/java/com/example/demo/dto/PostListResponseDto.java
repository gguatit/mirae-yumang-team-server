package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 목록 조회용 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponseDto {
    private Long id;
    private String title;
    private String author;
    private LocalDateTime createdAt;
    private int viewCount;
    private int likeCount;
}

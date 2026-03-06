package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 상세 조회용 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDetailResponseDto {
    private Long id;
    private String title;
    private String content;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;
    private int likeCount;
    private int hateCount;
    private List<String> imagePaths;
}

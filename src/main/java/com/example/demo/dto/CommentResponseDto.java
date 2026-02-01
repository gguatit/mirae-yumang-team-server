package com.example.demo.dto; // 본인의 패키지 경로에 맞게 수정

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private String content;
    private String username;
    private Long parentId;
    private String createdAt;
}
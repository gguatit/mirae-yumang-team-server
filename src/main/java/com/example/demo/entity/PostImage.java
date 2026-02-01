package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class PostImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName; // 저장된 실제 파일 이름
    private String filePath; // 브라우저 접근 경로

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post; // 어느 게시글의 이미지인지 연결

    // 기본 생성자
    public PostImage() {}

    // 편의 생성자
    public PostImage(String fileName, String filePath, Post post) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.post = post;
    }

    // -- Getter/Setter --
    public String getFilePath() {
        return filePath;
    }
}
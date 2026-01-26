package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "UNSAE")
@Getter
@NoArgsConstructor // JPA 필수
@AllArgsConstructor
@Builder
public class Unsae {

    // jsoup - .seiza-box 객체의 순서
    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // jsoup - .seiza-txt 내부 글자
    // 운세 대상 (별자리, 띠 등)
    @Column(name = "CON", nullable = false, length = 20)
    private String con;

    // jsoup - .period 내부 글자
    // 날짜 (문자열로 저장)
    @Column(name = "DATE", length = 20)
    private String date;

    // jsoup - .read 내부 글자
    // 전체 운세 내용
    @Column(name = "CONTENT", nullable = false, length = 255)
    private String content;

    // jsoup - .lucky-color-txt 내부 글자
    // 행운의 색
    @Column(name = "L_COR", nullable = false, length = 10)
    private String lCor;

    // jsoup - .key-txt 내부 글자
    // 행운의 키워드
    @Column(name = "L_KEY", nullable = false, length = 10)
    private String lKey;

    // jsoup - .icon-money 객체의 개수
    // 금전운
    @Column(name = "MONEY", nullable = false, length = 10)
    private String money;

    // jsoup - .icon-love 객체의 개수
    // 연애운
    @Column(name = "LOVE", nullable = false, length = 10)
    private String love;

    // jsoup - .icon-work 객체의 개수
    // 학업 / 업무운
    @Column(name = "STUDY", nullable = false, length = 10)
    private String study;

    // jsoup - .icon-health 객체의 개수
    // 건강운
    @Column(name = "HEALTH", nullable = false, length = 10)
    private String health;
}
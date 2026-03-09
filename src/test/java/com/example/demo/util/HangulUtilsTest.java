package com.example.demo.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HangulUtils 테스트
 * es-hangul과 유사한 기능이 Java에서 정상 작동하는지 검증
 */
class HangulUtilsTest {

    @Test
    void testIsHangul() {
        assertTrue(HangulUtils.isHangul('가'));
        assertTrue(HangulUtils.isHangul('한'));
        assertTrue(HangulUtils.isHangul('글'));
        assertFalse(HangulUtils.isHangul('a'));
        assertFalse(HangulUtils.isHangul('1'));
        assertFalse(HangulUtils.isHangul(' '));
    }

    @Test
    void testGetChoseong() {
        // 초성 추출 테스트
        assertEquals("ㅇㄴㅎㅅㅇ", HangulUtils.getChoseong("안녕하세요"));
        assertEquals("ㅎㄱ", HangulUtils.getChoseong("한글"));
        assertEquals("ㅅㄱ", HangulUtils.getChoseong("사과"));
        assertEquals("ㅂㄴㄴ", HangulUtils.getChoseong("바나나"));
        
        // 한글 아닌 문자는 그대로
        assertEquals("abc", HangulUtils.getChoseong("abc"));
        assertEquals("123", HangulUtils.getChoseong("123"));
        
        // 혼합
        assertEquals("ㅎㄱabc", HangulUtils.getChoseong("한글abc"));
    }

    @Test
    void testDisassemble() {
        // '한' → ['ㅎ', 'ㅏ', 'ㄴ']
        char[] result1 = HangulUtils.disassemble('한');
        assertArrayEquals(new char[]{'ㅎ', 'ㅏ', 'ㄴ'}, result1);
        
        // '가' → ['ㄱ', 'ㅏ'] (받침 없음)
        char[] result2 = HangulUtils.disassemble('가');
        assertArrayEquals(new char[]{'ㄱ', 'ㅏ'}, result2);
        
        // '글' → ['ㄱ', 'ㅡ', 'ㄹ']
        char[] result3 = HangulUtils.disassemble('글');
        assertArrayEquals(new char[]{'ㄱ', 'ㅡ', 'ㄹ'}, result3);
        
        // 한글 아닌 문자는 그대로
        char[] result4 = HangulUtils.disassemble('a');
        assertArrayEquals(new char[]{'a'}, result4);
    }

    @Test
    void testDisassembleToString() {
        assertEquals("ㅎㅏㄴㄱㅡㄹ", HangulUtils.disassembleToString("한글"));
        assertEquals("ㅇㅏㄴㄴㅕㅇ", HangulUtils.disassembleToString("안녕"));
        assertEquals("ㅅㅏㄱㅘ", HangulUtils.disassembleToString("사과"));  // "과"의 중성은 "ㅘ" 단일 문자
        
        // 혼합
        assertEquals("ㅎㅏㄴㄱㅡㄹabc123", HangulUtils.disassembleToString("한글abc123"));
    }

    @Test
    void testMatchesChoseong() {
        // 초성 매칭 테스트
        assertTrue(HangulUtils.matchesChoseong("안녕하세요", "ㅇㄴ"));
        assertTrue(HangulUtils.matchesChoseong("안녕하세요", "ㅇㄴㅎㅅㅇ"));
        assertTrue(HangulUtils.matchesChoseong("사과", "ㅅㄱ"));
        assertTrue(HangulUtils.matchesChoseong("바나나", "ㅂㄴ"));
        
        // 부분 매칭도 가능
        assertTrue(HangulUtils.matchesChoseong("자바스크립트", "ㅈㅂ"));
        assertTrue(HangulUtils.matchesChoseong("스프링부트", "ㅅㅍㄹ"));
        
        // 매칭 안 되는 경우
        assertFalse(HangulUtils.matchesChoseong("안녕하세요", "ㄱㄴ"));
        assertFalse(HangulUtils.matchesChoseong("사과", "ㅂㄴ"));
    }

    @Test
    void testQwertyToHangul() {
        // 영타 → 한글 자모 변환
        String result1 = HangulUtils.qwertyToHangul("gks");
        assertTrue(result1.contains("ㅎ") && result1.contains("ㅏ") && result1.contains("ㄴ"));
        
        String result2 = HangulUtils.qwertyToHangul("dkssud");
        assertTrue(result2.contains("ㅇ") && result2.contains("ㅏ") && result2.contains("ㄴ"));
        
        // 변환할 수 없는 문자는 그대로
        assertEquals("123", HangulUtils.qwertyToHangul("123"));
    }

    @Test
    void testEnhancedMatch() {
        // 1. 일반 텍스트 매칭
        assertTrue(HangulUtils.enhancedMatch("안녕하세요", "안녕"));
        assertTrue(HangulUtils.enhancedMatch("게시판 검색", "검색"));
        
        // 2. 초성 매칭
        assertTrue(HangulUtils.enhancedMatch("안녕하세요", "ㅇㄴ"));
        assertTrue(HangulUtils.enhancedMatch("스프링부트", "ㅅㅍ"));
        assertTrue(HangulUtils.enhancedMatch("자바 개발", "ㅈㅂ"));
        
        // 3. 자모 분리 매칭
        assertTrue(HangulUtils.enhancedMatch("한글", "ㅎㅏㄴ"));
        
        // 4. 대소문자 구분 안 함
        assertTrue(HangulUtils.enhancedMatch("Hello", "hello"));
        assertTrue(HangulUtils.enhancedMatch("JAVA", "java"));
        
        // 매칭 안 되는 경우
        assertFalse(HangulUtils.enhancedMatch("안녕하세요", "바나나"));
        assertFalse(HangulUtils.enhancedMatch("게시판", "댓글"));
    }

    @Test
    void testCalculateSearchScore() {
        // 정확히 일치하는 경우 높은 점수
        assertTrue(HangulUtils.calculateSearchScore("안녕", "안녕") > 50);
        
        // 시작 위치 일치
        assertTrue(HangulUtils.calculateSearchScore("안녕하세요", "안녕") > 30);
        
        // 포함되는 경우
        assertTrue(HangulUtils.calculateSearchScore("자바스크립트", "스크립트") > 0);
        
        // 초성 일치
        assertTrue(HangulUtils.calculateSearchScore("스프링", "ㅅㅍㄹ") > 0);
        
        // 매칭 안 되면 0점
        assertEquals(0, HangulUtils.calculateSearchScore("안녕", "바나나"));
    }

    @Test
    void testRealWorldSearchScenarios() {
        // 실제 게시판 검색 시나리오
        
        // 시나리오 1: 초성으로 검색
        assertTrue(HangulUtils.enhancedMatch("자바 개발자 모집", "ㅈㅂ"));
        assertTrue(HangulUtils.enhancedMatch("스프링 부트 강의", "ㅅㅍㄹ"));
        
        // 시나리오 2: 부분 단어로 검색
        assertTrue(HangulUtils.enhancedMatch("게시판 검색 기능 개선", "검색"));
        assertTrue(HangulUtils.enhancedMatch("한글 처리 라이브러리", "처리"));
        
        // 시나리오 3: 영타로 잘못 입력한 경우 (자모 변환)
        String koreanJamo = HangulUtils.qwertyToHangul("gks");
        assertTrue(koreanJamo.contains("ㅎ")); // 'g' → 'ㅎ'
        
        // 시나리오 4: 점수 비교 (더 관련성 높은 것이 높은 점수)
        int score1 = HangulUtils.calculateSearchScore("스프링 부트", "스프링");
        int score2 = HangulUtils.calculateSearchScore("자바 스프링", "스프링");
        assertTrue(score1 > score2); // "스프링 부트"가 시작 위치 일치로 더 높은 점수
    }

    @Test
    void testEdgeCases() {
        // null 처리
        assertFalse(HangulUtils.enhancedMatch(null, "test"));
        assertFalse(HangulUtils.enhancedMatch("test", null));
        assertEquals(0, HangulUtils.calculateSearchScore(null, "test"));
        
        // 빈 문자열
        assertEquals("", HangulUtils.getChoseong(""));
        assertEquals("", HangulUtils.disassembleToString(""));
        assertFalse(HangulUtils.enhancedMatch("", "test"));
        
        // 특수문자
        assertEquals("!@#", HangulUtils.getChoseong("!@#"));
        assertTrue(HangulUtils.enhancedMatch("테스트!@#", "테스트"));
    }
}

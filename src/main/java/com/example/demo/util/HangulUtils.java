package com.example.demo.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한글 처리 유틸리티 클래스
 * es-hangul 라이브러리의 기능을 Java로 구현
 * 
 * 주요 기능:
 * - 초성 추출 및 검색
 * - 자모 분리/조합
 * - 영타 ↔ 한글 변환
 * - 한글 검색 매칭
 */
public class HangulUtils {

    // 한글 유니코드 상수
    private static final int HANGUL_BASE = 0xAC00; // '가'
    private static final int HANGUL_END = 0xD7A3;  // '힣'
    
    // 초성, 중성, 종성 개수
    private static final int CHOSEONG_COUNT = 19;
    private static final int JUNGSEONG_COUNT = 21;
    private static final int JONGSEONG_COUNT = 28;
    
    // 초성 리스트 (19개)
    private static final char[] CHOSEONG_LIST = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    
    // 중성 리스트 (21개)
    private static final char[] JUNGSEONG_LIST = {
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
        'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };
    
    // 종성 리스트 (28개, 첫 번째는 받침 없음)
    private static final char[] JONGSEONG_LIST = {
        '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
        'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    // 영자판 → 한글 자모 매핑 (qwerty)
    private static final Map<Character, Character> QWERTY_TO_HANGUL = new HashMap<>();
    private static final Map<Character, Character> HANGUL_TO_QWERTY = new HashMap<>();
    
    static {
        // 자음
        QWERTY_TO_HANGUL.put('r', 'ㄱ'); QWERTY_TO_HANGUL.put('R', 'ㄲ');
        QWERTY_TO_HANGUL.put('s', 'ㄴ');
        QWERTY_TO_HANGUL.put('e', 'ㄷ'); QWERTY_TO_HANGUL.put('E', 'ㄸ');
        QWERTY_TO_HANGUL.put('f', 'ㄹ');
        QWERTY_TO_HANGUL.put('a', 'ㅁ');
        QWERTY_TO_HANGUL.put('q', 'ㅂ'); QWERTY_TO_HANGUL.put('Q', 'ㅃ');
        QWERTY_TO_HANGUL.put('t', 'ㅅ'); QWERTY_TO_HANGUL.put('T', 'ㅆ');
        QWERTY_TO_HANGUL.put('d', 'ㅇ');
        QWERTY_TO_HANGUL.put('w', 'ㅈ'); QWERTY_TO_HANGUL.put('W', 'ㅉ');
        QWERTY_TO_HANGUL.put('c', 'ㅊ');
        QWERTY_TO_HANGUL.put('z', 'ㅋ');
        QWERTY_TO_HANGUL.put('x', 'ㅌ');
        QWERTY_TO_HANGUL.put('v', 'ㅍ');
        QWERTY_TO_HANGUL.put('g', 'ㅎ');
        
        // 모음 (기본 모음만 매핑, 복합 모음은 제외)
        QWERTY_TO_HANGUL.put('k', 'ㅏ'); 
        QWERTY_TO_HANGUL.put('o', 'ㅐ');
        QWERTY_TO_HANGUL.put('i', 'ㅑ'); 
        QWERTY_TO_HANGUL.put('O', 'ㅒ');
        QWERTY_TO_HANGUL.put('j', 'ㅓ'); 
        QWERTY_TO_HANGUL.put('p', 'ㅔ');
        QWERTY_TO_HANGUL.put('u', 'ㅕ'); 
        QWERTY_TO_HANGUL.put('P', 'ㅖ');
        QWERTY_TO_HANGUL.put('h', 'ㅗ');
        QWERTY_TO_HANGUL.put('y', 'ㅛ'); 
        QWERTY_TO_HANGUL.put('n', 'ㅜ');
        QWERTY_TO_HANGUL.put('b', 'ㅠ');
        QWERTY_TO_HANGUL.put('m', 'ㅡ');
        QWERTY_TO_HANGUL.put('l', 'ㅣ');
        
        // 역매핑 생성
        QWERTY_TO_HANGUL.forEach((key, value) -> HANGUL_TO_QWERTY.put(value, key));
    }

    /**
     * 한글 문자인지 확인
     */
    public static boolean isHangul(char ch) {
        return ch >= HANGUL_BASE && ch <= HANGUL_END;
    }

    /**
     * 초성인지 확인
     */
    public static boolean isChoseong(char ch) {
        for (char c : CHOSEONG_LIST) {
            if (c == ch) return true;
        }
        return false;
    }

    /**
     * 문자열에서 초성만 추출
     * 예: "안녕하세요" → "ㅇㄴㅎㅅㅇ"
     */
    public static String getChoseong(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (isHangul(ch)) {
                int index = ch - HANGUL_BASE;
                int choseongIndex = index / (JUNGSEONG_COUNT * JONGSEONG_COUNT);
                result.append(CHOSEONG_LIST[choseongIndex]);
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 한글 문자를 초성, 중성, 종성으로 분리
     * 예: '한' → ['ㅎ', 'ㅏ', 'ㄴ']
     */
    public static char[] disassemble(char ch) {
        if (!isHangul(ch)) {
            return new char[]{ch};
        }
        
        int index = ch - HANGUL_BASE;
        int choseongIndex = index / (JUNGSEONG_COUNT * JONGSEONG_COUNT);
        int jungseongIndex = (index % (JUNGSEONG_COUNT * JONGSEONG_COUNT)) / JONGSEONG_COUNT;
        int jongseongIndex = index % JONGSEONG_COUNT;
        
        if (jongseongIndex == 0) {
            return new char[]{
                CHOSEONG_LIST[choseongIndex],
                JUNGSEONG_LIST[jungseongIndex]
            };
        } else {
            return new char[]{
                CHOSEONG_LIST[choseongIndex],
                JUNGSEONG_LIST[jungseongIndex],
                JONGSEONG_LIST[jongseongIndex]
            };
        }
    }

    /**
     * 문자열 전체를 자모로 분리
     * 예: "한글" → "ㅎㅏㄴㄱㅡㄹ"
     */
    public static String disassembleToString(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (isHangul(ch)) {
                for (char jamo : disassemble(ch)) {
                    result.append(jamo);
                }
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 초성 검색 매칭
     * 예: matchesChoseong("사과", "ㅅㄱ") → true
     */
    public static boolean matchesChoseong(String text, String choseongQuery) {
        if (text == null || choseongQuery == null) {
            return false;
        }
        
        String textChoseong = getChoseong(text.toLowerCase());
        String query = choseongQuery.toLowerCase();
        
        return textChoseong.contains(query);
    }

    /**
     * 영타를 한글로 변환
     * 예: "gksrmf" → "한글"
     * 주의: 완벽한 변환이 아닌 기본 매핑만 지원
     */
    public static String qwertyToHangul(String qwerty) {
        if (qwerty == null || qwerty.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (char ch : qwerty.toCharArray()) {
            result.append(QWERTY_TO_HANGUL.getOrDefault(ch, ch));
        }
        return result.toString();
    }

    /**
     * 한글 자모를 영타로 변환
     * 예: "ㅎㅏㄴㄱㅡㄹ" → "gksrmf"
     */
    public static String hangulToQwerty(String hangul) {
        if (hangul == null || hangul.isEmpty()) {
            return "";
        }
        
        String disassembled = disassembleToString(hangul);
        StringBuilder result = new StringBuilder();
        
        for (char ch : disassembled.toCharArray()) {
            result.append(HANGUL_TO_QWERTY.getOrDefault(ch, ch));
        }
        return result.toString();
    }

    /**
     * 향상된 검색 매칭
     * - 일반 텍스트 매칭
     * - 초성 매칭
     * - 자모 분리 매칭
     * - 영타 매칭
     */
    public static boolean enhancedMatch(String text, String query) {
        if (text == null || query == null) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        // 1. 일반 텍스트 매칭
        if (lowerText.contains(lowerQuery)) {
            return true;
        }
        
        // 2. 초성 매칭 (전체 초성이거나 일부 초성)
        if (matchesChoseong(lowerText, lowerQuery)) {
            return true;
        }
        
        // 3. 자모 분리 매칭
        String disassembledText = disassembleToString(lowerText);
        if (disassembledText.contains(lowerQuery)) {
            return true;
        }
        
        // 4. 영타로 입력한 경우 (예: "gksmf" → "한글")
        String hangulConverted = qwertyToHangul(lowerQuery);
        if (lowerText.contains(hangulConverted)) {
            return true;
        }
        
        // 5. 한글을 영타로 변환해서 매칭
        String qwertyConverted = hangulToQwerty(lowerText);
        if (qwertyConverted.contains(lowerQuery)) {
            return true;
        }
        
        return false;
    }

    /**
     * 검색 점수 계산 (관련도 순 정렬용)
     * 높을수록 더 관련성이 높음
     */
    public static int calculateSearchScore(String text, String query) {
        if (text == null || query == null) {
            return 0;
        }
        
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int score = 0;
        
        // 정확히 일치: 100점
        if (lowerText.equals(lowerQuery)) {
            return 100;
        }
        
        // 시작 위치 일치: 50점
        if (lowerText.startsWith(lowerQuery)) {
            score += 50;
        }
        
        // 일반 포함: 30점
        if (lowerText.contains(lowerQuery)) {
            score += 30;
        }
        
        // 초성 일치: 20점
        if (matchesChoseong(lowerText, lowerQuery)) {
            score += 20;
        }
        
        // 자모 분리 일치: 15점
        String disassembled = disassembleToString(lowerText);
        if (disassembled.contains(lowerQuery)) {
            score += 15;
        }
        
        // 영타 변환 일치: 10점
        String hangulConverted = qwertyToHangul(lowerQuery);
        if (lowerText.contains(hangulConverted)) {
            score += 10;
        }
        
        return score;
    }
}

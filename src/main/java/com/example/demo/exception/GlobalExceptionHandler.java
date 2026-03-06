package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException e, RedirectAttributes redirectAttributes) {
        log.warn("리소스 조회 실패: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/posts";
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException e, RedirectAttributes redirectAttributes) {
        log.warn("권한 없음: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/auth/login";
    }
}

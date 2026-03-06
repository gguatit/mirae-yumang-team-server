package com.example.demo.exception;

/**
 * 권한이 없을 때 발생하는 예외
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        super("권한이 없습니다.");
    }
}

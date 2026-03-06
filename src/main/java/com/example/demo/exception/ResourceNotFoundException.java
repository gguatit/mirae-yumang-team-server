package com.example.demo.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * (게시글, 사용자, 댓글 등)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + "을(를) 찾을 수 없습니다. (ID: " + id + ")");
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(resourceName + "을(를) 찾을 수 없습니다. (" + identifier + ")");
    }
}

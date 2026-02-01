package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.CommentResponseDto;
import com.example.demo.service.CommentService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/posts")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/{postId}/comments")
    @ResponseBody
    public ResponseEntity<?> saveComment(   
            @PathVariable Long postId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId, // 👈 대댓글일 경우 부모 ID를 받음 (일반 댓글은 null)
            HttpSession session) {

        String username = (String) session.getAttribute("loginUser");
        

        // 💡 로그인이 안 되어 있다면 401 에러 반환
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        
        // 서비스에서 댓글 저장 후 저장된 정보를 DTO로 받아옴
        CommentResponseDto response = commentService.saveComment(postId, username, content, parentId);

        return ResponseEntity.ok(response);
    }
}

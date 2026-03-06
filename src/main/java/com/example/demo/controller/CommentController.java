package com.example.demo.controller;

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
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/posts")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
    @ResponseBody
    public ResponseEntity<?> saveComment(
            @PathVariable Long postId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentId,
            HttpSession session) {

        String username = (String) session.getAttribute("loginUser");

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        CommentResponseDto response = commentService.saveComment(postId, username, content, parentId);
        return ResponseEntity.ok(response);
    }
}

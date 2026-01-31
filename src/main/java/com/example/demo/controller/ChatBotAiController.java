package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatBotAiController {

    @GetMapping("/chatai")
    public String chatAiPage(HttpSession session, Model model) {
        // 세션에서 username을 가져옵니다.
        String username = (String) session.getAttribute("loginUser");

        // username을 모델에 담아서 템플릿으로 전달합니다.
        model.addAttribute("username", username);

        return "chatai";  // "chatai.html"로 템플릿 렌더링
    }
}
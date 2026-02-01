package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;


@Controller
public class AnnouncementController {
    
    @GetMapping("/announcement")
    public String announcementPage(HttpSession session, Model model) {
       String username = (String) session.getAttribute("loginUser");

        // username을 모델에 담아서 템플릿으로 전달합니다.
        model.addAttribute("username", username);

        return "announcement";  // "chatai.html"로 템플릿 렌더링
    }
}
package com.example.demo.controller;


import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.crawler.CrawlingUtils;
import com.example.demo.entity.Unsae;
import com.example.demo.service.UnsaeService;

import ch.qos.logback.core.model.Model;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/unsae") //이 매핑으로 다 시작
public class UnsaeController {

    @Autowired
    private UnsaeService unsaeService;

    @GetMapping("/patch")
    public List<Unsae> test(HttpSession session) {
        return unsaeService.patchUnsaes();  // templates/patch.html 렌더링
    }
}

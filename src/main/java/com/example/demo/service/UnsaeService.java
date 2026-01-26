package com.example.demo.service;


import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.crawler.CrawlingUtils;
import com.example.demo.entity.Post;
import com.example.demo.entity.Unsae;
import com.example.demo.repository.UnsaeRepository;

@Service

public class UnsaeService {

    @Autowired
    private UnsaeRepository unsaeRepository;

    public List<Unsae> patchUnsaes() {
        
        String viewerUrl = "https://www.tv-asahi.co.jp/goodmorning/uranai/";

        try {
            List<Unsae> unsaeList = CrawlingUtils.crawlUnsae(viewerUrl);
            unsaeRepository.saveAll(unsaeList);
            return unsaeList;
        } catch (IOException e) {
            System.err.println("크롤링 실패: " + e.getMessage());
        }

        return null;
    }
}

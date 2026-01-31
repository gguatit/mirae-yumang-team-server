package com.example.demo.service;

import com.example.demo.entity.Lh;
import com.example.demo.entity.Post;
import com.example.demo.entity.RecommendationType;
import com.example.demo.entity.User;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LhService {
    private final LhRepository lhRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public void toggleLikeHate(Long userId, Long postId, RecommendationType type) {
        // 기존 기록 조회
        Optional<Lh> alreadyLH = lhRepository.findByUserIdAndPostId(userId, postId);

        if (alreadyLH.isPresent()) {
            Lh existing = alreadyLH.get();
            if (existing.getType() == type) {
                lhRepository.delete(existing); // 같은 타입이면 취소
                return;
            } else {
                existing.changeType(type); // 다른 타입이면 변경 (Entity에 메서드 필요)
                return;
            }
        }

        // 기록 없으면 신규 생성
        User user = userRepository.findById(userId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        
        lhRepository.save(Lh.builder().user(user).post(post).type(type).build());
        return;
    }
}
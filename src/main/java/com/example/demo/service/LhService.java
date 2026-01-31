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
        Post post = postRepository.findById(postId).orElseThrow();
        Optional<Lh> alreadyLH = lhRepository.findByUserIdAndPostId(userId, postId);

        if (alreadyLH.isPresent()) {
            Lh existing = alreadyLH.get();
            if (existing.getType() == type) {
                // 같은 버튼 클릭 시: 취소 (숫자 -1)
                if (type == RecommendationType.L) post.updateLikeCount(-1);
                else post.updateHateCount(-1);
                lhRepository.delete(existing);
            } else {
                // 다른 버튼 클릭 시: 변경 (한쪽 -1, 다른쪽 +1)
                if (type == RecommendationType.L) {
                    post.updateLikeCount(1);
                    post.updateHateCount(-1);
                } else {
                    post.updateLikeCount(-1);
                    post.updateHateCount(1);
                }
                existing.changeType(type);
            }
        } else {
            // 처음 클릭 시: 생성 (숫자 +1)
            User user = userRepository.findById(userId).orElseThrow(); // 유저 정보 가져오기

            if (type == RecommendationType.L) post.updateLikeCount(1);
            else post.updateHateCount(1);

            // 🔥 실제 DB에 기록을 남기는 코드가 반드시 있어야 합니다!
            Lh newLh = Lh.builder()
                    .user(user)
                    .post(post)
                    .type(type)
                    .build();
            
            lhRepository.save(newLh); 
        }
    }
}
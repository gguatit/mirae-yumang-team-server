package com.example.demo.service;

import com.example.demo.entity.Lh;
import com.example.demo.entity.Post;
import com.example.demo.entity.RecommendationType;
import com.example.demo.entity.User;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
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
                log.info("추천/비추천 취소: postId={}, type={}", postId, type);
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
                log.info("추천/비추천 변경: postId={}, type={}", postId, type);
            }
        } else {
            // 처음 클릭 시: 생성 (숫자 +1)
            User user = userRepository.findById(userId).orElseThrow();

            if (type == RecommendationType.L) post.updateLikeCount(1);
            else post.updateHateCount(1);

            Lh newLh = Lh.builder()
                    .user(user)
                    .post(post)
                    .type(type)
                    .build();

            lhRepository.save(newLh);
            log.info("추천/비추천 생성: postId={}, type={}", postId, type);
        }
    }
}
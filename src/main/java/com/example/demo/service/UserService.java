package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final LhRepository lhRepository;
    private final CommentRepository commentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 이메일 중복 여부 확인 (Controller에서 별도 에러 메시지 분기용)
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 회원가입
     *
     * @return 성공 시 true, 중복 시 false
     */
    public boolean registerUser(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            return false;
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword, email);
        userRepository.save(user);
        log.info("회원가입 성공: {}", username);
        return true;
    }

    /**
     * 로그인 검증
     *
     * @return 성공 시 User 객체, 실패 시 null
     */
    public User authenticateUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }

        return null;
    }

    /**
     * 사용자명으로 사용자 조회 (마이페이지 등에서 사용)
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자", username));
    }

    /**
     * 자기소개 수정
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateBio(String username, String bio) {
        User user = getUserByUsername(username);
        // XSS 방지: HTML 태그 제거
        String safeBio = (bio != null) ? Jsoup.clean(bio, Safelist.none()) : "";
        user.setBio(safeBio);
        userRepository.save(user);
        log.info("자기소개 수정: {}", username);
    }

    /**
     * 프로필 이미지 경로 수정
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateProfileImage(String username, String profileImagePath) {
        User user = getUserByUsername(username);
        user.setProfileImagePath(profileImagePath);
        userRepository.save(user);
        log.info("프로필 이미지 수정: {}", username);
    }

    /**
     * 회원 탈퇴
     * 1. 비밀번호 재확인 (본인 인증)
     * 2. 해당 유저의 Lh(좋아요/싫어요) 삭제
     * 3. 해당 유저의 Comment 삭제
     * 4. 해당 유저의 Post 삭제 (CascadeType.ALL → PostImage, Comment, Lh 자동 삭제)
     * 5. 프로필 이미지 파일 삭제
     * 6. User 삭제
     *
     * @return 성공 시 true, 비밀번호 불일치 시 false
     */
    @org.springframework.transaction.annotation.Transactional
    public boolean deleteUser(String username, String rawPassword) {
        User user = getUserByUsername(username);

        // 비밀번호 재확인
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("회원 탈퇴 실패 - 비밀번호 불일치: {}", username);
            return false;
        }

        // 1. 이 유저가 남긴 Lh 기록 삭제 (Post FK 제약 전 선처리)
        lhRepository.deleteByUser(user);

        // 2. 이 유저가 남긴 댓글 삭제 (Post FK 제약 전 선처리)
        commentRepository.deleteByUser(user);

        // 3. 이 유저의 게시글 삭제 (CascadeType.ALL → PostImage, 게시글 단위 Lh/Comment 자동 삭제)
        List<Post> posts = postRepository.findByUserOrderByCreatedAtDesc(user);
        postRepository.deleteAll(posts);

        // 4. 프로필 이미지 파일 삭제
        String profileImagePath = user.getProfileImagePath();
        if (profileImagePath != null && !profileImagePath.isBlank()) {
            try {
                // profileImagePath 예: /upload/profile/uuid.jpg → 실제 경로로 변환
                String relativePath = profileImagePath.replaceFirst("^/upload/", "");
                File profileFile = new File(uploadDir + relativePath);

                // Path Traversal 방지: 파일이 uploadDir 하위에 있는지 canonical path로 검증
                String canonicalUploadDir = new File(uploadDir).getCanonicalPath();
                String canonicalFilePath = profileFile.getCanonicalPath();

                if (canonicalFilePath.startsWith(canonicalUploadDir) && profileFile.isFile()) {
                    profileFile.delete();
                    log.info("프로필 이미지 파일 삭제: {}", canonicalFilePath);
                } else {
                    log.warn("프로필 이미지 경로 이상 감지 (Path Traversal 방지): {}", profileImagePath);
                }
            } catch (java.io.IOException e) {
                log.warn("프로필 이미지 경로 처리 중 오류 (탈퇴는 계속 진행): {}", e.getMessage());
            }
        }

        // 5. User 삭제
        userRepository.delete(user);
        log.info("회원 탈퇴 완료: {}", username);
        return true;
    }
}
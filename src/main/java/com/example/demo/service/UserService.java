package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

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
}
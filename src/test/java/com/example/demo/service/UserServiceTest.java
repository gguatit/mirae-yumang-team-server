package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // 테스트마다 초기화
    }

    @Test
    @DisplayName("회원가입 - 성공")
    void registerUser_Success() {
        // given
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(new User("newuser", "encodedPassword", "new@test.com"));

        // when
        boolean result = userService.registerUser("newuser", "password123", "new@test.com");

        // then
        assertThat(result).isTrue();
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("회원가입 - 중복 사용자명")
    void registerUser_DuplicateUsername() {
        // given
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // when
        boolean result = userService.registerUser("existinguser", "password", "test@test.com");

        // then
        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 - 성공")
    void authenticateUser_Success() {
        // given
        User user = new User("testuser", "encodedPassword", "test@test.com");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        // when
        User result = userService.authenticateUser("testuser", "rawPassword");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("로그인 - 잘못된 비밀번호")
    void authenticateUser_WrongPassword() {
        // given
        User user = new User("testuser", "encodedPassword", "test@test.com");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // when
        User result = userService.authenticateUser("testuser", "wrongPassword");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 사용자")
    void authenticateUser_UserNotFound() {
        // given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when
        User result = userService.authenticateUser("unknown", "password");

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자 조회 - 성공")
    void getUserByUsername_Success() {
        // given
        User user = new User("testuser", "encodedPassword", "test@test.com");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // when
        User result = userService.getUserByUsername("testuser");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("사용자 조회 - 존재하지 않는 사용자")
    void getUserByUsername_NotFound() {
        // given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

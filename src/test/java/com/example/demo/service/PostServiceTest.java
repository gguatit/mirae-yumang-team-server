package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.LhRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LhRepository lhRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private PostService postService;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "encodedPassword", "test@test.com");
        testPost = new Post("테스트 제목", "테스트 내용", testUser);
    }

    @Test
    @DisplayName("게시글 생성 - 성공")
    void createPost_Success() {
        // given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Post result = postService.createPost("새 제목", "새 내용", "testuser");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("새 제목");
        assertThat(result.getContent()).isEqualTo("새 내용");
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 생성 - 존재하지 않는 사용자")
    void createPost_UserNotFound() {
        // given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.createPost("제목", "내용", "unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 조회 - 성공 (조회수 증가)")
    void getPostById_Success() {
        // given
        testPost.setViewCount(5);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        // when
        Post result = postService.getPostById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getViewCount()).isEqualTo(6); // 조회수 1 증가
        verify(postRepository, times(1)).save(testPost);
    }

    @Test
    @DisplayName("게시글 조회 - 존재하지 않는 게시글")
    void getPostById_NotFound() {
        // given
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPostById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updatePost_Success() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        // when
        boolean result = postService.updatePost(1L, "수정된 제목", "수정된 내용", "testuser", null, null, null);

        // then
        assertThat(result).isTrue();
        assertThat(testPost.getTitle()).isEqualTo("수정된 제목");
        assertThat(testPost.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("게시글 수정 - 권한 없음")
    void updatePost_Unauthorized() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        // when
        boolean result = postService.updatePost(1L, "수정", "내용", "otheruser", null, null, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void deletePost_Success() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        doNothing().when(postRepository).delete(testPost);

        // when
        boolean result = postService.deletePost(1L, "testuser");

        // then
        assertThat(result).isTrue();
        verify(postRepository, times(1)).delete(testPost);
    }

    @Test
    @DisplayName("게시글 삭제 - 권한 없음")
    void deletePost_Unauthorized() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        // when
        boolean result = postService.deletePost(1L, "otheruser");

        // then
        assertThat(result).isFalse();
        verify(postRepository, never()).delete(any());
    }

    @Test
    @DisplayName("게시글 삭제 - 존재하지 않는 게시글")
    void deletePost_NotFound() {
        // given
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        // when
        boolean result = postService.deletePost(999L, "testuser");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("XSS 방지 - HTML 태그 제거")
    void createPost_XssPrevention() {
        // given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Post result = postService.createPost(
                "<script>alert('xss')</script>제목",
                "<b>굵은</b> 내용",
                "testuser");

        // then
        assertThat(result.getTitle()).doesNotContain("<script>");
        assertThat(result.getContent()).doesNotContain("<b>");
    }
}

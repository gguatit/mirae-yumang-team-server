package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 관련 DB 접근 계층 (Repository)
 *
 * 학습 포인트:
 * - JpaRepository를 상속하면 CRUD와 페이징 관련 메서드를 자동으로 제공받음
 * - 제네릭의 두 번째 파라미터는 엔티티의 ID 타입과 일치해야 함 (User.id는 Long)
 * - Optional을 활용해 널(null) 안전성을 높이는 패턴
 *
 * 주의: 여기서 JpaRepository<User, Integer>로 잘못 선언되어 있으면
 * 컴파일이나 런타임에서 타입 불일치 문제가 발생할 수 있음.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // username 중복 체크 (회원가입 시 사용)
    boolean existsByUsername(String username);

    // username으로 사용자 조회 (로그인, 마이페이지 등에서 사용)
    // 반환 타입이 Optional이므로 호출부에서 안전하게 처리해야 함
    Optional<User> findByUsername(String username);

    // email 중복 체크 (회원가입 시 사용)
    boolean existsByEmail(String email);
}

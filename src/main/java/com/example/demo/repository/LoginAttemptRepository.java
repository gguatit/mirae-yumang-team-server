package com.example.demo.repository;

import com.example.demo.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {

    Optional<LoginAttemptEntity> findByClientKey(String clientKey);

    void deleteByClientKey(String clientKey);

    // 만료된 기록 정리용
    void deleteByLastAttemptAtBefore(Instant before);
}

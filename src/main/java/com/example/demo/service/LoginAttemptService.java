package com.example.demo.service;

import com.example.demo.entity.LoginAttemptEntity;
import com.example.demo.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 로그인 시도 횟수를 DB에 저장하여 서버 재시작 후에도 잠금이 유지되도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 15 * 60; // 15분

    private final LoginAttemptRepository repository;

    /**
     * 해당 키가 잠금 상태인지 확인합니다.
     * 잠금 시간이 지난 경우 자동으로 해제합니다.
     */
    @Transactional
    public boolean isLocked(String key) {
        return repository.findByClientKey(key)
                .map(attempt -> {
                    if (attempt.getLockUntil() == null) return false;
                    if (Instant.now().isAfter(attempt.getLockUntil())) {
                        repository.deleteByClientKey(key);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * 로그인 실패를 기록합니다.
     * MAX_ATTEMPTS 이상 실패 시 계정을 잠급니다.
     */
    @Transactional
    public void recordFailure(String key) {
        LoginAttemptEntity attempt = repository.findByClientKey(key)
                .orElseGet(() -> {
                    LoginAttemptEntity a = new LoginAttemptEntity();
                    a.setClientKey(key);
                    a.setAttempts(0);
                    return a;
                });

        attempt.setAttempts(attempt.getAttempts() + 1);
        attempt.setLastAttemptAt(Instant.now());

        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            attempt.setLockUntil(Instant.now().plusSeconds(LOCK_DURATION_SECONDS));
            log.warn("로그인 잠금 활성화: {} ({}회 실패)", key, attempt.getAttempts());
        }

        repository.save(attempt);
    }

    /**
     * 로그인 성공 시 시도 기록을 초기화합니다.
     */
    @Transactional
    public void clearFailures(String key) {
        repository.deleteByClientKey(key);
    }

    /**
     * 24시간 이상 지난 오래된 기록을 1시간마다 정리합니다.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cleanupExpiredAttempts() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        repository.deleteByLastAttemptAtBefore(cutoff);
        log.debug("로그인 시도 기록 정리 완료");
    }
}

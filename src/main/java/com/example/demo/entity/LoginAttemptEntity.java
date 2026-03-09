package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
public class LoginAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_key", nullable = false, unique = true, length = 255)
    private String clientKey;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "lock_until")
    private Instant lockUntil;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt;
}

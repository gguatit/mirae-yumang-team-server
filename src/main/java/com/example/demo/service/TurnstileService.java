package com.example.demo.service;

import com.example.demo.dto.TurnstileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${turnstile.secret.key}")
    private String secretKey;

    /**
     * Turnstile 토큰을 검증합니다.
     *
     * @param token     클라이언트로부터 받은 Turnstile 토큰
     * @param remoteIp  클라이언트 IP 주소
     * @return 검증 성공 여부
     */
    public boolean validateToken(String token, String remoteIp) {
        if (token == null || token.isBlank()) {
            log.warn("Turnstile 토큰이 없습니다.");
            return false;
        }

        if (token.length() > 2048) {
            log.warn("Turnstile 토큰이 너무 깁니다. 길이: {}", token.length());
            return false;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", secretKey);
            map.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                map.add("remoteip", remoteIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<TurnstileResponse> response = restTemplate.postForEntity(
                    VERIFY_URL, request, TurnstileResponse.class);

            TurnstileResponse result = response.getBody();
            if (result == null) {
                log.error("Turnstile 응답이 null입니다.");
                return false;
            }

            if (result.isSuccess()) {
                log.info("Turnstile 검증 성공: hostname={}", result.getHostname());
                return true;
            } else {
                log.warn("Turnstile 검증 실패: error-codes={}", result.getErrorCodes());
                return false;
            }

        } catch (Exception e) {
            log.error("Turnstile 검증 중 오류 발생", e);
            return false;
        }
    }
}

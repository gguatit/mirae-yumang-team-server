# SECURITY

STARLOG 프로젝트의 보안 정책 및 취약점 보고 절차를 설명합니다.

---

## 지원 버전

| 버전 | 지원 여부 |
| --- | --- |
| 1.0.x | 지원 |
| < 1.0 | 미지원 |

---

## 보안 취약점 보고

보안 취약점을 발견하신 경우, 공개 이슈 트래커 대신 다음 방법으로 비공개로 보고해주세요:

### 연락처

- **Email**: dev@kalpha.kr
- **Repository**: [mirae-yumang-team](https://github.com/mirae-yumang-team-project/mirae-yumang-team)

### 보고 시 포함할 정보

1. 취약점 유형 (XSS, SQL Injection, CSRF 등)
2. 영향받는 컴포넌트 또는 기능
3. 재현 단계 (Step-by-step)
4. 예상되는 영향 및 심각도
5. 가능한 경우, 개념 증명(PoC) 코드

### 응답 시간

- 24시간 내 보고 확인
- 7일 내 초기 평가 및 대응 계획 제공
- 심각도에 따라 패치 개발 및 배포

---

## 구현된 보안 기능

### 인증 및 권한

- **BCrypt 비밀번호 암호화**: BCryptPasswordEncoder 사용, 강도 10
- **세션 기반 인증**: HttpSession을 통한 사용자 인증
- **세션 타임아웃**: 운영 환경 60분, 개발 환경 30분
- **로그아웃 보안**: POST 방식으로만 로그아웃 가능, 세션 완전 무효화
- **브루트포스 방어**: IP+계정 조합으로 5회 실패 시 15분 잠금, DB 영속 저장으로 서버 재시작 후에도 유지
- **만료 기록 자동 정리**: `@Scheduled`로 24시간 이상 된 로그인 시도 기록 매 1시간마다 삭제

### 입력 검증

- **사용자명**: 3-20자, 영문/숫자/언더스코어
- **비밀번호**: 최소 8자, 최대 100자
- **이메일**: 정규식 기반 형식 검증 + 중복 등록 차단
- **게시글 제목**: 최대 200자
- **게시글 내용**: 최대 10,000자
- **댓글**: 최대 1,000자
- **자기소개**: 최대 500자

### XSS 방지

- **Jsoup 1.18.3**: HTML 태그 완전 제거 (Safelist.none())
- **게시글/댓글/자기소개**: 사용자 입력 모든 HTML 필터링
- **Thymeleaf**: 자동 HTML 이스케이핑
- **JavaScript**: DOM 삽입 시 `textContent` 및 `escapeHtml()` 사용

### 파일 업로드 보안

- **확장자 검증**: jpg, jpeg, png, gif, webp만 허용
- **MIME 타입 검증**: Content-Type 헤더 확인
- **파일 크기 제한**: 게시글 이미지 최대 10MB, 프로필 이미지 최대 5MB
- **안전한 파일명**: UUID 기반 파일명 생성
- **저장 경로 격리**: 웹 루트 외부에 저장

### 데이터베이스 보안

- **SQL Injection 방지**: JPA Prepared Statement 사용
- **비밀번호 별도 관리**: 환경 변수로 DB 자격증명 관리 (폴백 기본값 없음)
- **연결 암호화**: MySQL SSL 연결 지원

### API 보안

- **조회수 어뷰징 방지**: 세션당 게시글 1회만 조회수 증가
- **좋아요/싫어요 인증**: 비로그인 사용자 401 반환
- **게시글 수정/삭제 권한 검증**: 서버 측에서 작성자 여부 확인
- **댓글 대댓글 무결성**: 부모 댓글이 동일 게시글 소속인지 검증
- **`/posts/api/new` Rate Limit**: IP별 2초에 1회 요청으로 제한
- **API 정보 최소화**: JSON 응답에서 `email`, `bio`, `profileImagePath` 제거

### 네트워크 보안

- **HTTPS**: Cloudflare Tunnel을 통한 SSL/TLS 암호화
- **Secure Cookie**: HTTPS에서 쿠키 전송
- **SameSite Cookie**: Strict 정책으로 CSRF 추가 방어
- **Content-Security-Policy**: 인라인 스크립트 및 외부 리소스 제한
- **HSTS**: 1년 유지, 서브도메인 포함
- **Referrer-Policy**: `strict-origin-when-cross-origin`

### 세션 보안

- **HTTP-Only 쿠키**: JavaScript 접근 차단
- **세션 고정 공격 방지**: 로그인 시 기존 세션 무효화 후 새 세션 발급

---

## 보안 설정

### 운영 환경

```properties
# 쿠키 보안
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict
server.servlet.session.cookie.http-only=true

# 세션 타임아웃
server.servlet.session.timeout=60m

# SQL 로그 비활성화
spring.jpa.show-sql=false

# H2 콘솔 비활성화
spring.h2.console.enabled=false
```

### 환경 변수

민감한 정보는 환경 변수로 관리 (폴백 기본값 없이 반드시 설정 필요):

```bash
export DB_USERNAME=starlog_user
export DB_PASSWORD=secure_password
```

> ⚠️ `DB_PASSWORD` 환경 변수가 설정되지 않으면 애플리케이션이 시작되지 않습니다.

---

## 보안 체크리스트

### 배포 전 확인사항

- [ ] 모든 비밀번호가 BCrypt로 암호화되는가?
- [ ] `DB_PASSWORD` 환경 변수가 설정되었는가? (폴백 기본값 없음)
- [ ] H2 콘솔이 비활성화되었는가?
- [ ] SQL 로그가 비활성화되었는가?
- [ ] HTTPS가 적용되었는가?
- [ ] Secure 쿠키 설정이 활성화되었는가?
- [ ] 파일 업로드 크기 제한이 설정되었는가?
- [ ] 민감한 파일이 .gitignore에 포함되었는가?
- [ ] `login_attempts` 테이블이 DB에 생성되었는가?

### 정기 점검사항

- [ ] 의존성 취약점 검사 (월 1회)
- [ ] 데이터베이스 백업 확인 (주 1회)
- [ ] 접근 로그 검토 (주 1회)
- [ ] 세션 타임아웃 테스트
- [ ] 파일 업로드 제한 테스트

---

## 알려진 제한사항

### 현재 구현되지 않은 기능

1. **이메일 인증**: 회원가입 시 이메일 소유 여부 검증 없음
2. **2FA**: 2단계 인증 미지원
3. **감사 로그**: 관리자 수준의 사용자 활동 감사 로그 없음
4. **CAPTCHA**: 자동화 공격 방지 CAPTCHA 없음

### 권장 사항

- 프로덕션 환경에서는 방화벽 설정 필수
- 정기적인 보안 패치 및 업데이트
- 정기적인 데이터베이스 백업
- 접근 로그 모니터링
- 민감한 정보 노출 방지

---

## 보안 업데이트 이력

### 2026-03-09 (v2)

- **[HIGH]** `User` JSON 직렬화에서 `email`, `bio`, `profileImagePath` 제거 — API 정보 노출 차단
- **[HIGH]** 대댓글 `parentId` 교차 게시글 오염 방지 — 부모 댓글 소속 게시글 검증 추가
- **[MEDIUM]** 브루트포스 방어를 인메모리 → DB 영속 저장으로 전환 (`login_attempts` 테이블)
- **[MEDIUM]** 세션 기반 조회수 중복 방지 — 세션당 게시글 1회만 `viewCount` 증가
- **[MEDIUM]** 이메일 중복 등록 차단 — `existsByEmail` 검증 추가
- **[LOW]** `application-prod.properties` DB 패스워드 폴백 기본값 제거
- **[LOW]** `/posts/api/new` Rate Limit 추가 — IP별 2초에 1회 제한

### 2026-03-09 (v1)

- MySQL/MariaDB 마이그레이션 완료
- 파일 업로드 MIME 타입 검증 추가
- 입력 검증 강화 (사용자명, 비밀번호, 이메일)
- Jsoup 1.18.3으로 업데이트
- Cloudflare Tunnel HTTPS 적용
- Secure 및 SameSite 쿠키 설정
- Content-Security-Policy 헤더 추가
- HSTS 및 Referrer-Policy 설정

### 초기 버전

- BCrypt 비밀번호 암호화
- 세션 기반 인증
- XSS 방지 기본 구현
- 파일 업로드 확장자 검증

---

## 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [SECURITY_GUIDE.md](SECURITY_GUIDE.md) - 상세 보안 가이드

---

## 책임 공개

이 프로젝트는 교육 및 학습 목적으로 개발되었습니다. 프로덕션 환경에서 사용할 경우 추가적인 보안 검토가 필요합니다.

# SECURITY

STARLOG 프로젝트의 보안 정책 및 취약점 보고 절차를 설명합니다.

---

## 지원 버전

| 버전 | 지원 여부 |
| --- | --- |
| 1.3.x | 지원 (최신) |
| 1.2.x | 지원 |
| 1.1.x | 지원 |
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
- **GitHub OAuth2 소셜 로그인**: Spring Security OAuth2 Client, 계정 연동 지원
- **세션 기반 인증**: HttpSession을 통한 사용자 인증
- **세션 타임아웃**: 운영 환경 60분, 개발 환경 30분
- **로그아웃 보안**: POST 방식으로만 로그아웃 가능, 세션 완전 무효화
- **브루트포스 방어**: IP+계정 조합으로 5회 실패 시 15분 잠금, DB 영속 저장
- **세션 고정 공격 방지**: 로그인/OAuth 연동 시 기존 세션 무효화 후 새 세션 발급
- **만료 기록 자동 정리**: @Scheduled로 24시간 이상 된 로그인 시도 기록 매 1시간마다 삭제

### 입력 검증
- **사용자명**: 3-20자, 영문/숫자/언더스코어
- **비밀번호**: 최소 8자, 최대 100자 (BCrypt DoS 방지)
- **이메일**: 정규식 기반 형식 검증 + 중복 등록 차단
- **게시글 제목**: 최대 200자
- **게시글 내용**: 최대 10,000자
- **댓글**: 최대 1,000자
- **자기소개**: 최대 500자

### XSS 방지
- **Jsoup 1.18.3**: HTML 태그 완전 제거 (Safelist.none())
- **게시글/댓글/자기소개**: 사용자 입력 모든 HTML 필터링
- **Thymeleaf**: 자동 HTML 이스케이핑
- **JavaScript**: DOM 삽입 시 textContent 및 escapeHtml() 사용
- **CSP**: Content-Security-Policy 적용

### 파일 업로드 보안
- **확장자 검증**: jpg, jpeg, png, gif, webp만 허용
- **MIME 타입 검증**: Content-Type 헤더 확인
- **파일 크기 제한**: 게시글 이미지 최대 10MB, 프로필 이미지 최대 5MB
- **안전한 파일명**: UUID 기반 파일명 생성
- **저장 경로 격리**: 웹 루트 외부에 저장
- **Path Traversal 방지**: canonical path 검증으로 업로드 디렉터리 외부 접근 차단

### Cloudflare Turnstile (CAPTCHA)
- 로그인 및 회원가입 폼에 Cloudflare Turnstile 적용
- 서버 측 siteverify API 검증
- 토큰 길이 검증 (최대 2048자)
- 실패 시 폼 제출 차단 및 재시도 유도

### API 보안
- 조회수 어뷰징 방지: 세션당 게시글 1회만 조회수 증가
- 좋아요/싫어요 인증: 비로그인 사용자 401 반환
- 게시글 수정/삭제 권한 검증: 서버 측에서 작성자 여부 확인
- 댓글 대댓글 무결성: 부모 댓글이 동일 게시글 소속인지 검증
- /posts/api/new Rate Limit: IP별 2초에 1회 요청으로 제한

### 네트워크 보안
- **HTTPS**: Cloudflare Tunnel을 통한 SSL/TLS 암호화
- **Secure Cookie**: HTTPS에서 쿠키 전송
- **SameSite Cookie**: Strict 정책으로 CSRF 추가 방어
- **Content-Security-Policy**: 인라인 스크립트 및 외부 리소스 제한
- **HSTS**: 1년 유지, 서브도메인 포함
- **Referrer-Policy**: strict-origin-when-cross-origin
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
export TURNSTILE_SECRET_KEY=your_turnstile_secret
export GITHUB_CLIENT_ID=your_github_oauth_client_id
export GITHUB_CLIENT_SECRET=your_github_oauth_client_secret
```

> DB_PASSWORD 환경 변수가 설정되지 않으면 애플리케이션이 시작되지 않습니다.

---

## Git 보안

### Git에 커밋하면 안 되는 파일들

다음 파일들은 민감한 정보가 포함되어 있어 .gitignore에 추가되어 있습니다:

**설정 파일:**
- `src/main/resources/application-prod.properties` - 운영 환경 DB 정보
- `.env`, `.env.local`, `.env.prod` - 환경 변수 파일

**시스템 파일:**
- `*.service` - DB 비밀번호 등이 포함된 서비스 파일
- `starlog.service`, `cloudflared.service`

**Cloudflare 인증 정보:**
- `.cloudflared/` 디렉토리 전체 (cert.pem, *.json, config.yml)

**데이터/빌드 산출물:**
- `upload/`, `starlog/` - 애플리케이션 데이터
- `*.sql`, `*.sql.gz` - 데이터베이스 백업
- `*.jar` - 빌드된 JAR 파일
- `target/` - Maven 빌드 디렉토리
- `*.log` - 로그 파일

### 안전한 설정 방법

템플릿 파일을 복사하여 실제 설정 파일 생성:
```bash
cp src/main/resources/application-prod.properties.template \
   src/main/resources/application-prod.properties
cp starlog.service.template starlog.service
```

### 커밋 전 확인사항
```bash
# 민감한 파일이 추적되는지 확인
git status

# .gitignore가 제대로 작동하는지 확인
git check-ignore -v *.service

# 커밋할 파일 목록 확인
git diff --cached --name-only
```

### 실수로 커밋한 경우

아직 push하지 않았다면:
```bash
git reset --soft HEAD~1
git reset HEAD sensitive-file.properties
echo "sensitive-file.properties" >> .gitignore
git add .gitignore
git commit -m "Add sensitive file to .gitignore"
```

이미 push했다면 즉시 비밀번호를 변경하고 BFG Repo-Cleaner로 history에서 제거하세요.

---

## 보안 체크리스트

### 배포 전 확인사항
- [ ] 모든 비밀번호가 BCrypt로 암호화되는가?
- [ ] DB_PASSWORD 환경 변수가 설정되었는가?
- [ ] H2 콘솔이 비활성화되었는가?
- [ ] SQL 로그가 비활성화되었는가?
- [ ] HTTPS가 적용되었는가?
- [ ] Secure 쿠키 설정이 활성화되었는가?
- [ ] 파일 업로드 크기 제한이 설정되었는가?
- [ ] 민감한 파일이 .gitignore에 포함되었는가?
- [ ] TURNSTILE_SECRET_KEY 환경 변수가 설정되었는가?

### 정기 점검사항
- [ ] 의존성 취약점 검사 (월 1회)
- [ ] 데이터베이스 백업 확인 (주 1회)
- [ ] 접근 로그 검토 (주 1회)
- [ ] 세션 타임아웃 테스트
- [ ] 파일 업로드 제한 테스트

---

## 알려진 제한사항

1. **이메일 인증**: 회원가입 시 이메일 소유 여부 검증 없음
2. **2FA**: 2단계 인증 미지원
3. **감사 로그**: 관리자 수준의 사용자 활동 감사 로그 없음
4. **Subresource Integrity (SRI)**: 외부 CDN 리소스(Spline, Turnstile)에 SRI 해시 미적용

---

## 보안 업데이트 이력

### 2026-07-14 (v6)
- GitHub OAuth2 소셜 로그인 추가 -- Spring Security OAuth2 Client 기반
- 계정 연동 기능: 기존 계정에 GitHub 계정 연결 (세션 기반 연동)
- OAuth2 사용자 로그인 시 신규 계정 자동 생성
- 세션 고정 공격 방지: OAuth 로그인/연동 시 기존 세션 무효화

### 2026-04-24 (v5)
- Cloudflare Turnstile 도입 -- 로그인/회원가입에 봇 방지 및 자동화 공격 차단 적용
- Spline 3D Viewer 추가 -- 외부 CDN 리소스 사용
- 모바일 환경 애니메이션 분리 -- 성능 및 보안 안정성 향상
- CSS overflow 속성 개선
- 업로드 파일 캐시 제어 강화

### 2026-03-11 (v3)
- 회원 탈퇴 기능 구현 -- 비밀번호 재확인, 연관 데이터 삭제, Path Traversal 방지
- 회원 탈퇴 브루트포스 방어 -- LoginAttemptService 재사용
- BCrypt DoS 방지 -- 비밀번호 입력 100자 초과 시 즉시 거부
- 게시글 수정 시 이미지 관리 구현
- 정적 리소스 브라우저 캐싱 (30일)

### 2026-03-09 (v2)
- User JSON 직렬화에서 email, bio, profileImagePath 제거
- 대댓글 parentId 교차 게시글 오염 방지
- 브루트포스 방어를 인메모리 -> DB 영속 저장으로 전환
- 세션 기반 조회수 중복 방지
- 이메일 중복 등록 차단
- application-prod.properties DB 패스워드 폴백 기본값 제거
- /posts/api/new Rate Limit 추가

### 2026-03-09 (v1)
- MySQL/MariaDB 마이그레이션
- 파일 업로드 MIME 타입 검증
- 입력 검증 강화
- Jsoup 1.18.3 업데이트
- Cloudflare Tunnel HTTPS 적용
- Secure/SameSite 쿠키, CSP, HSTS, Referrer-Policy 설정

---

## 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [docs/guide/service-management.md](docs/guide/service-management.md) - 서비스 관리 가이드

---

## 책임 공개

이 프로젝트는 교육 및 학습 목적으로 개발되었습니다. 프로덕션 환경에서 사용할 경우 추가적인 보안 검토가 필요합니다.

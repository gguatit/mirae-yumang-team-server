# SECURITY

STARLOG 프로젝트의 보안 현황을 정리한 문서입니다.

---

## 구현 완료된 보안 기능

### 1. 비밀번호 암호화

- BCryptPasswordEncoder를 사용하여 비밀번호를 해싱 저장
- 로그인 시 matches()로 안전한 비교 수행
- 평문 비밀번호가 DB에 저장되지 않음

### 2. CSRF (Cross-Site Request Forgery) 보호

- Spring Security의 CSRF 토큰 기능 활성화
- 모든 POST 폼에 th:action을 사용하여 CSRF 토큰 자동 삽입
- AJAX 요청(좋아요/싫어요, 댓글)에 CSRF 토큰 헤더 포함
- H2 콘솔은 개발 편의상 CSRF 예외 처리

### 3. XSS (Cross-Site Scripting) 방지

- Jsoup.clean()을 사용하여 게시글 제목/본문에서 HTML 태그 제거
- 댓글 내용에도 동일한 필터링 적용
- Safelist.none() 정책으로 모든 HTML 태그 차단

### 4. 파일 업로드 보안

- 허용된 이미지 확장자만 업로드 가능: jpg, jpeg, png, gif, webp
- 화이트리스트 방식으로 검증하여 악성 파일 차단
- UUID 기반 파일명 변환으로 원본 파일명 노출 방지

### 5. 로그아웃 보안

- GET 방식 로그아웃 제거, POST 방식만 허용
- 로그아웃 시 세션 전체 무효화 (session.invalidate)
- 모든 페이지의 로그아웃 버튼이 CSRF 토큰이 포함된 POST 폼으로 동작

### 6. 세션 관리

- 세션 타임아웃 30분 설정
- HTTP-Only 쿠키 설정으로 JavaScript에서 세션 쿠키 접근 차단
- 세션 기반 사용자 인증 및 권한 확인

### 7. 입력값 검증

- 회원가입 시 아이디 필수, 비밀번호 최소 4자 검증
- 게시글 작성/수정 시 제목, 내용 빈 값 검증
- 게시글 수정/삭제 시 작성자 본인 확인

---

## 향후 구현 필요한 보안 기능

### 우선순위 높음

- **Spring Security 인증/인가 체계 도입**: 현재 수동 세션 관리를 Spring Security의 인증 체계로 전환
- **비밀번호 정책 강화**: 최소 8자, 영문/숫자/특수문자 조합 요구
- **이메일 검증**: 회원가입 시 이메일 인증 절차 추가
- **파일 업로드 MIME 타입 검증**: 확장자 외에 실제 파일 내용 기반 타입 검증

### 우선순위 중간

- **Rate Limiting**: 로그인 시도 횟수 제한 (무차별 대입 공격 방지)
- **세션 고정 공격 방지**: 로그인 성공 시 세션 ID 재생성
- **HTTPS 적용**: SSL/TLS 인증서 도입 및 Secure 쿠키 설정
- **SameSite 쿠키 설정**: CSRF 추가 방어를 위한 쿠키 정책

### 우선순위 낮음

- **H2 콘솔 비활성화**: 프로덕션 환경에서 반드시 비활성화
- **프로덕션 DB 전환**: H2에서 MySQL/PostgreSQL로 전환
- **보안 헤더 추가**: Content-Security-Policy, X-Content-Type-Options 등
- **감사 로그**: 로그인/로그아웃/게시글 변경 이력 기록
- **API 인증**: REST API 엔드포인트에 JWT 기반 인증 도입

# STARLOG

> 오늘의 운세 관련 코드와 각종 정보들은 [Today-s-horoscope](https://github.com/gguatit/Today-s-horoscope) 레포지토리에서 확인할 수 있습니다.

> 학교 커뮤니티 기반 웹 애플리케이션

미래유망팀의 Spring Boot 기반 통합 커뮤니티 플랫폼입니다. 게시판, 한글 검색, AI 챗봇, 학교 정보 페이지 등 다양한 기능을 제공합니다.

## 빌드 & 서버 재시작

> **⚠️ systemd (`sudo systemctl restart starlog`)는 사용하지 마세요.**
>
> systemd 서비스는 환경변수/stdout flush/TIME_WAIT 문제로 새 인스턴스가 hang 하거나
> `Restart=always` + `RestartSec=5` 루프가 정상 인스턴스를 SIGKILL로 죽입니다.
> **반드시 아래 수동 재시작 절차만 사용하세요.**

### 1. 빌드

```bash
./mvnw clean package -DskipTests
```

### 2. 기존 인스턴스 정지

```bash
# 8090 포트를 잡고 있는 java 프로세스만 정확히 추출 (cloudflared 등 무관)
PID=$(sudo lsof -ti:8090 2>/dev/null | while read p; do
  [ "$(cat /proc/$p/comm 2>/dev/null)" = "java" ] && echo "$p"
done | head -1)
if [ -n "$PID" ]; then sudo kill "$PID"; fi
sleep 5
ss -tlnp 2>&1 | grep 8090
# 8090이 비어 있어야 다음 단계 진행
```

> **절대 `kill -9` 사용 금지.** Spring Boot이 graceful shutdown 중 코너 케이스 데이터가 손실될 수 있습니다.
> `kill` (SIGTERM) 만 사용. 30초 안에 안 죽으면 `kill -9`로 escalation.

### 3. 새 인스턴스 수동 기동

```bash
nohup sudo -u kalpha env \
  DB_USERNAME=starlog_user \
  DB_PASSWORD=starlog_2026_recover \
  TURNSTILE_SECRET_KEY=0x4AAAAAADCFFKe73OfGNIrj4H0qywF7geo \
  /usr/bin/java -jar /home/kalpha/mirae-yumang-team-server/target/demo-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  > /tmp/starlog.log 2>&1 &
disown
```

### 4. 검증

```bash
sleep 20
ss -tlnp 2>&1 | grep 8090          # java가 8090 listen 중이어야 함
curl -s -o /dev/null -w "HTTP %{http_code}\n" --max-time 5 http://localhost:8090/home  # 200
curl -s http://localhost:8090/home | grep "home.css?v="  # 최신 캐시 버전 확인
tail -5 /tmp/starlog.log            # "Started DemoApplication" + "홈 접속" 로그
```

### 5. systemd 상태 (참고용)

```bash
sudo systemctl status starlog --no-pager   # 현재 disabled
sudo systemctl is-enabled starlog          # disabled
```

재부팅 후 자동 시작이 꼭 필요하면 systemd를 살려야 하지만, `StandardOutput=append:/var/log/starlog/app.log`,
`TimeoutStopSec=60`, `RestartSec=10` 등 추가 설정이 필요합니다.

### 한 줄 요약 (자주 하는 실수 방지)

```bash
# ✅ 이렇게 하세요
./mvnw clean package -DskipTests && \
  PID=$(sudo lsof -ti:8090 2>/dev/null | while read p; do [ "$(cat /proc/$p/comm 2>/dev/null)" = "java" ] && echo "$p"; done | head -1) && \
  [ -n "$PID" ] && sudo kill "$PID" && sleep 5 && \
  nohup sudo -u kalpha env DB_USERNAME=starlog_user DB_PASSWORD=starlog_2026_recover TURNSTILE_SECRET_KEY=0x4AAAAAADCFFKe73OfGNIrj4H0qywF7geo \
  /usr/bin/java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > /tmp/starlog.log 2>&1 & disown

# ❌ 절대 하지 마세요
sudo systemctl restart starlog     # systemd hang, 8090 안 잡힘
sudo kill -9 <PID>                  # 정상 인스턴스 강제 종료, 데이터 손실 위험
```

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 접속 URL

- **운영 서버**: https://starlog.c01.kr
- **로컬 개발**: http://localhost:8090

## 운영 DB 접속 정보

> 학교 프로젝트 — 보안 무관, 팀 공유용으로 기록

| 항목 | 값 |
|------|-----|
| **Host** | `localhost:3306` |
| **Database** | `starlog` |
| **Username** | `starlog_user` |
| **Password** | `starlog_2026_recover` |

```bash
mysql -h localhost -u starlog_user -p starlog
# Enter password: starlog_2026_recover
```

비밀번호 변경 시:

```bash
ALTER USER 'starlog_user'@'localhost' IDENTIFIED BY 'new_password';
FLUSH PRIVILEGES;
```

---

## Features

### 사용자 인증 및 보안
- 회원가입, 로그인, 로그아웃, 세션 관리
- BCrypt 비밀번호 암호화 (입력 100자 제한으로 DoS 방지)
- 브루트포스 방어: IP+계정 조합 5회 실패 시 15분 잠금 (DB 영속 저장)
- Cloudflare Turnstile: 로그인/회원가입 시 봇 방지
- 입력 검증 (사용자명 3-20자, 비밀번호 8-100자, 이메일 형식)
- XSS 방지 (Jsoup 1.18.3 HTML 태그 제거)
- 회원 탈퇴: 비밀번호 재확인 후 계정 및 모든 데이터 삭제

### 게시판 시스템
- 게시글 CRUD (이미지 추가/삭제 포함), 조회수 증가
- 다중 이미지 첨부
- 좋아요/싫어요 기능
- 실시간 업데이트 (5초 자동 폴링, 신규 게시글)
- 인기 게시글 실시간 갱신 (30초 폴링)
- 검색 및 정렬 (최신순, 인기순, 조회순)
- 서버사이드 페이지네이션

### 한글 검색 기능
- 초성 검색: "ㅅㄱ" 입력 시 "스프링" 검색
- 자모 분리 검색: 유사 단어 매칭
- 영타 자동 변환: "tkdls" -> "사람"
- 기본/고급 검색 모드 지원

### 댓글 시스템
- 게시글별 댓글 작성 및 조회
- 대댓글 지원 (부모 댓글 소속 게시글 검증)
- 댓글/대댓글에 작성자 프로필 이미지 표시

### 프로필 및 마이페이지
- 프로필 이미지 업로드 및 변경 (jpg, png, gif, webp)
- 자기소개 수정
- 내 게시글 목록
- 회원 탈퇴 기능

### 학교 정보 페이지
- 근명고등학교 급식 정보 (NEIS API)
- 학년/반 선택 시간표 (NEIS 시간표 API)
- 이번 달 학사일정 (NEIS API)
- 인메모리 캐싱 (ConcurrentHashMap)
- 아침 7시 자동 캐시 갱신

### 파일 업로드
- 이미지 업로드 (jpg, png, gif, webp)
- 파일 크기 제한 (게시글 10MB, 프로필 5MB)
- MIME 타입 검증
- 안전한 파일명 생성 (UUID)
- Path Traversal 방지 (canonical path 검증)

### 성능
- 정적 리소스 브라우저 캐싱 (CSS/JS/이미지: 30일)
- GSAP 기반 페이지 진입 애니메이션 (커뮤니티 페이지)
- 모바일/데스크톱 최적화 반응형 디자인

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| **Backend** | Spring Boot 3.5.7, Java 17 |
| **Web Framework** | Spring MVC |
| **Security** | Spring Security, BCrypt |
| **Template Engine** | Thymeleaf |
| **ORM** | Spring Data JPA, Hibernate |
| **Database** | MySQL 8.0 / MariaDB 11.8.3, H2 (dev/test) |
| **Build Tool** | Maven |
| **Server** | Tomcat (Embedded), systemd |
| **Deployment** | Cloudflare Tunnel |
| **Frontend** | Vanilla JS, GSAP 3.12.5, ScrollTrigger |
| **3D/Interactive** | Spline Viewer |
| **External APIs** | NEIS (급식/시간표/학사일정) |
| **External Libraries** | Jsoup 1.18.3, Lombok |
| **CAPTCHA** | Cloudflare Turnstile |
| **Custom Utilities** | HangulUtils (한글 검색) |

---

## Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+** (또는 포함된 Maven Wrapper 사용)
- **MySQL 8.0+** 또는 **MariaDB 10.3+**
- **Git**

### Installation

```bash
git clone https://github.com/mirae-yumang-team-project/mirae-yumang-team.git
cd mirae-yumang-team
```

MySQL/MariaDB 설정은 [docs/guide/mysql-setup.md](docs/guide/mysql-setup.md)를 참조하세요.

### Running

#### 개발 환경 (H2 데이터베이스)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 운영 환경 (MySQL/MariaDB)
```bash
export DB_USERNAME=starlog_user
export DB_PASSWORD=starlog_2026_recover
export NEIS_API_KEY=your_neis_api_key
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### JAR 파일로 실행
```bash
./mvnw package -DskipTests
DB_USERNAME=starlog_user DB_PASSWORD=starlog_2026_recover \
NEIS_API_KEY=your_neis_api_key \
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

서버가 시작되면 http://localhost:8090 으로 접속하세요.

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java          # 애플리케이션 진입점
│   │   ├── config/
│   │   │   ├── SecurityConfig.java       # 보안 설정
│   │   │   ├── WebConfig.java            # 웹 설정
│   │   │   └── NeisConfig.java           # NEIS RestTemplate 설정
│   │   ├── controller/
│   │   │   ├── HomeController.java
│   │   │   ├── AuthController.java
│   │   │   ├── PostController.java
│   │   │   ├── CommentController.java
│   │   │   └── SchoolController.java     # /school 페이지
│   │   ├── service/
│   │   │   ├── UserService.java
│   │   │   ├── PostService.java
│   │   │   ├── CommentService.java
│   │   │   ├── LhService.java
│   │   │   ├── LoginAttemptService.java
│   │   │   └── NeisService.java          # NEIS API + 캐싱
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── ... (Post/User DTOs)
│   │   │   └── neis/
│   │   │       ├── NeisMealRow.java
│   │   │       └── NeisScheduleRow.java
│   │   ├── exception/
│   │   └── util/
│   │       └── HangulUtils.java          # 한글 검색 유틸
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       ├── static/
│       │   ├── css/
│       │   │   ├── ... (기존 스타일)
│       │   │   └── school.css            # 학교 페이지 스타일
│       │   ├── js/
│       │   └── images/
│       └── templates/
│           ├── home.html
│           ├── login.html
│           ├── post-list.html
│           ├── school.html               # 학교 정보 페이지
│           └── ...
└── test/
```

---

## Architecture

```
Client -> Controller -> Service -> Repository -> Database
           |
         View (Thymeleaf)
```

| Layer | Responsibility |
|-------|---------------|
| **Controller** | HTTP 요청 처리, 라우팅 |
| **Service** | 비즈니스 로직, 트랜잭션 관리 |
| **Repository** | 데이터 영속성, CRUD |
| **Entity** | 도메인 모델, DB 매핑 |
| **View** | 화면 렌더링 (Thymeleaf) |

---

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /auth/login | 로그인 페이지 |
| POST | /auth/login | 로그인 처리 |
| GET | /auth/register | 회원가입 페이지 |
| POST | /auth/register | 회원가입 처리 |
| POST | /auth/logout | 로그아웃 |
| GET | /auth/delete-account | 회원 탈퇴 페이지 |
| POST | /auth/delete-account | 회원 탈퇴 처리 |

### Posts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /posts | 게시글 목록 (검색, 정렬, 페이지네이션) |
| GET | /posts/{id} | 게시글 상세 |
| GET | /posts/write | 게시글 작성 페이지 |
| POST | /posts/write | 게시글 작성 (이미지 첨부) |
| GET | /posts/{id}/edit | 게시글 수정 페이지 |
| POST | /posts/{id}/edit | 게시글 수정 |
| POST | /posts/{id}/delete | 게시글 삭제 |
| POST | /api/{postId}/like-hate | 좋아요/싫어요 |
| GET | /posts/api/new?since={datetime} | 실시간 새 게시글 조회 |
| GET | /posts/api/best | 인기 게시글 조회 |

### School
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /school?grade=&classNum= | 급식 + 시간표 + 학사일정 |

### User
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /home | 홈페이지 |
| GET | /mypage | 마이페이지 |
| POST | /mypage/bio | 자기소개 수정 |
| POST | /mypage/profile-image | 프로필 이미지 변경 |

### Others
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /chatai | AI 챗봇 |

---

## Database

### Entity Relationships
```
User (1) <-> (N) Post
Post (1) <-> (N) Comment
Post (1) <-> (N) PostImage
Post (1) <-> (N) Lh (Like/Hate)
User (1) <-> (N) Comment
User (1) <-> (N) Lh
```

### Tables
- `users` - 사용자 정보
- `posts` - 게시글
- `comments` - 댓글
- `post_image` - 게시글 이미지
- `lh` - 좋아요/싫어요 기록
- `login_attempts` - 로그인 시도 기록

---

## Related Documentation

- [docs/guide/service-management.md](docs/guide/service-management.md) - systemd 서비스 관리 가이드
- [docs/guide/mysql-setup.md](docs/guide/mysql-setup.md) - MySQL/MariaDB 설정 가이드
- [docs/guide/hangul-search.md](docs/guide/hangul-search.md) - 한글 검색 기능 사용법
- [SECURITY.md](SECURITY.md) - 보안 정책 및 설정

---

## Project Status

### Version: 1.3.0

### Completed Features
- 사용자 인증 (회원가입, 로그인, 로그아웃)
- 회원 탈퇴 (비밀번호 재확인, 연관 데이터 전체 삭제)
- 게시판 CRUD
- 게시글 이미지 수정
- 한글 검색 (초성, 자모, 영타 변환)
- 댓글 시스템 (대댓글, 프로필 이미지 표시)
- 파일 업로드 (Path Traversal 방지)
- 좋아요/싫어요 기능
- 마이페이지 (프로필 이미지, 자기소개)
- 학교 정보 페이지 (급식, 시간표, 학사일정)
- MySQL/MariaDB 마이그레이션
- Cloudflare Tunnel 배포
- systemd 서비스 자동 시작
- 실시간 새 게시글 폴링 (5초) / 인기글 갱신 (30초)
- GSAP 애니메이션
- 정적 리소스 브라우저 캐싱 (30일)
- Spline 3D 인터랙티브 배경
- Cloudflare Turnstile
- 서버사이드 페이지네이션

### Test Coverage
- 28 unit tests passing
- PostServiceTest: 10 tests
- HangulUtilsTest: 10 tests
- UserServiceTest: 7 tests
- Integration tests: 1 test

---

## Contact

**Team**: 미래유망팀
**Repository**: [mirae-yumang-team](https://github.com/mirae-yumang-team-project/mirae-yumang-team)
**Live Site**: https://starlog.c01.kr

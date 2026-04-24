> 오늘의 운세 관련 코드와 각종 정보들은 [Today-s-horoscope](https://github.com/gguatit/Today-s-horoscope) 레포지토리에서 확인할 수 있습니다.

# STARLOG
```
./mvnw clean package -DskipTests && sudo systemctl restart starlog (서버 재시작)
```
> 학교 커뮤니티 기반 웹 애플리케이션

미래유망팀의 Spring Boot 기반 통합 커뮤니티 플랫폼입니다. 게시판, 한글 검색, AI 챗봇 등 다양한 기능을 제공합니다.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 접속 URL

- **운영 서버**: https://starlog.c01.kr
- **로컬 개발**: http://localhost:8090

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Database](#database)
- [Development](#development)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Overview

STARLOG는 학교 커뮤니티를 위한 종합 웹 플랫폼으로, 게시글 작성/조회, 사용자 인증, 파일 업로드, AI 챗봇 등의 기능을 제공합니다. Spring Boot의 MVC 아키텍처와 계층형 구조를 기반으로 설계되었습니다.

### Key Highlights

- 완전한 CRUD 기능을 갖춘 게시판 시스템
- 세션 기반 사용자 인증 및 권한 관리
- 이미지 업로드 및 파일 관리
- 반응형 UI (Thymeleaf)
- Spline 3D 인터랙티브 배경
- 모바일/데스크톱 최적화된 반응형 디자인

---

## Features

### 사용자 인증 및 보안
- 회원가입, 로그인, 로그아웃, 세션 관리
- BCrypt 비밀번호 암호화 (입력 100자 제한으로 DoS 방지)
- 브루트포스 방어: IP+계정 조합 5회 실패 시 15분 잠금 (DB 영속 저장)
- **Cloudflare Turnstile**: 로그인/회원가입 시 봇 방지 및 자동화 공격 차단
- 입력 검증 (사용자명 3-20자, 비밀번호 8-100자, 이메일 형식)
- XSS 방지 (Jsoup 1.18.3 HTML 태그 제거)
- **회원 탈퇴**: 비밀번호 재확인 후 계정 및 모든 데이터 삭제

### 게시판 시스템
- 게시글 작성, 수정(이미지 추가/삭제 포함), 삭제, 조회수 증가
- 다중 이미지 첨부 (게시글 수정 시 기존 이미지 삭제 및 신규 이미지 추가 가능)
- 좋아요/싫어요 기능
- 실시간 업데이트 (5초 자동 폴링, 신규 게시글)
- 인기 게시글 실시간 갱신 (30초 폴링)
- 검색 및 정렬 (최신순, 인기순, 조회순)
- 서버사이드 페이지네이션

### 한글 검색 기능
- 초성 검색: "ㅅㄱ" 입력 시 "스프링" 검색
- 자모 분리 검색: 유사 단어 매칭
- 영타 자동 변환: "tkdls" → "사람"
- 기본/고급 검색 모드 지원

### 댓글 시스템
- 게시글별 댓글 작성 및 조회
- 대댓글 지원 (부모 댓글 소속 게시글 검증)
- 댓글/대댓글에 작성자 프로필 이미지 표시 (GIF 애니메이션 포함)

### 프로필 및 마이페이지
- 프로필 이미지 업로드 및 변경 (jpg, png, gif, webp)
- 자기소개 수정
- 내 게시글 목록
- **회원 탈퇴** 기능

### 파일 업로드
- 이미지 업로드 (jpg, png, gif, webp)
- 파일 크기 제한 (게시글 10MB, 프로필 5MB)
- MIME 타입 검증
- 안전한 파일명 생성 (UUID)
- Path Traversal 방지 (canonical path 검증)

### 성능
- 정적 리소스 브라우저 캐싱 (CSS/JS/이미지: 30일, Cache-Control)
- 업로드 파일 캐시 제어 및 정적 리소스 핸들러 설정 개선
- GSAP 기반 페이지 진입 애니메이션 (커뮤니티 페이지)
- 동적으로 추가되는 게시글/인기 게시글에도 애니메이션 적용
- 모바일 환경에서 애니메이션 로직 분리로 콘텐츠 가시성 향상

### 기타
- AI 챗봇 인터페이스
- GSAP + ScrollTrigger 애니메이션
- Spline 3D 인터랙티브 배경 (홈페이지)
- 반응형 디자인 (모바일/태블릿/데스크톱)
- 사이트 파비콘 (둥근 로고)
- CSS 최적화 및 스크롤 동작 개선

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
| **External Libraries** | Jsoup 1.18.3, Lombok |
| **CAPTCHA/Bot Protection** | Cloudflare Turnstile |
| **Custom Utilities** | HangulUtils (한글 검색) |

---

## Getting Started

### Prerequisites

다음 소프트웨어가 설치되어 있어야 합니다:

- **Java 17** or higher
- **Maven 3.6+** (또는 포함된 Maven Wrapper 사용)
- **MySQL 8.0+** 또는 **MariaDB 10.3+**
- **Git**

### Installation

1. 저장소 클론

```bash
git clone https://github.com/mirae-yumang-team-project/mirae-yumang-team.git
cd mirae-yumang-team
```

2. 데이터베이스 설정

```bash
# MySQL/MariaDB 접속
sudo mysql

# 데이터베이스 및 사용자 생성
CREATE DATABASE IF NOT EXISTS starlog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'starlog_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON starlog.* TO 'starlog_user'@'localhost';
FLUSH PRIVILEGES;
```

3. 환경 변수 설정

```bash
export DB_USERNAME=starlog_user
export DB_PASSWORD=your_password
```

4. 의존성 설치

```bash
./mvnw clean install
```

### Running the Application

#### 개발 환경 (H2 데이터베이스)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 운영 환경 (MySQL/MariaDB)

```bash
# 환경 변수 설정
export DB_USERNAME=starlog_user
export DB_PASSWORD=your_password

# 서버 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### JAR 파일로 실행

```bash
./mvnw package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

#### systemd 서비스로 실행 (추천)

```bash
# 서비스 시작
sudo systemctl start starlog.service

# 서비스 상태 확인
sudo systemctl status starlog.service

# 로그 확인
sudo journalctl -u starlog.service -f
```

서버가 시작되면 브라우저에서 다음 URL로 접속:

```
http://localhost:8090
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java          # 애플리케이션 진입점
│   │   ├── config/
│   │   │   ├── SecurityConfig.java       # 보안 설정
│   │   │   └── WebConfig.java            # 웹 설정 (파일 업로드 경로, 캐시 등)
│   │   ├── controller/                   # HTTP 요청 처리
│   │   │   ├── HomeController.java
│   │   │   ├── AuthController.java
│   │   │   ├── PostController.java
│   │   │   ├── CommentController.java
│   │   │   └── ...
│   │   ├── service/                      # 비즈니스 로직
│   │   │   ├── UserService.java
│   │   │   ├── PostService.java
│   │   │   ├── CommentService.java
│   │   │   ├── LhService.java
│   │   │   ├── LoginAttemptService.java
│   │   │   └── ...
│   │   ├── repository/                   # 데이터 접근 계층
│   │   │   ├── UserRepository.java
│   │   │   ├── PostRepository.java
│   │   │   ├── CommentRepository.java
│   │   │   ├── LhRepository.java
│   │   │   ├── LoginAttemptRepository.java
│   │   │   └── ...
│   │   ├── entity/                       # JPA 엔티티
│   │   │   ├── User.java
│   │   │   ├── Post.java
│   │   │   ├── PostImage.java
│   │   │   ├── Comment.java
│   │   │   ├── Lh.java
│   │   │   ├── LoginAttemptEntity.java
│   │   │   └── ...
│   │   ├── dto/                          # 데이터 전송 객체
│   │   │   ├── PostListResponseDto.java
│   │   │   ├── PostDetailResponseDto.java
│   │   │   ├── PostWriteRequestDto.java
│   │   │   └── CommentResponseDto.java
│   │   ├── exception/                    # 예외 처리
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── UnauthorizedException.java
│   │   └── util/
│   │       └── HangulUtils.java          # 한글 검색 유틸
│   └── resources/
│       ├── application.properties        # 공통 설정
│       ├── application-dev.properties    # 개발 환경
│       ├── application-prod.properties   # 운영 환경
│       ├── static/                       # 정적 리소스
│       │   ├── css/
│       │   ├── js/
│       │   └── images/
│       └── templates/                    # Thymeleaf 템플릿
│           ├── home.html
│           ├── login.html
│           ├── post-list.html
│           └── ...
└── test/                                 # 테스트 코드
```

---

## Architecture

### MVC Pattern

이 프로젝트는 전형적인 Spring MVC 패턴을 따릅니다:

```
Client → Controller → Service → Repository → Database
           ↓
         View (Thymeleaf)
```

#### Layer Responsibilities

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **Controller** | HTTP 요청 처리, 라우팅 | `PostController.java` |
| **Service** | 비즈니스 로직, 트랜잭션 관리 | `PostService.java` |
| **Repository** | 데이터 영속성, CRUD | `PostRepository.java` |
| **Entity** | 도메인 모델, DB 매핑 | `Post.java`, `User.java` |
| **View** | 화면 렌더링 | `post-list.html` |

### Data Flow Example

1. 사용자가 게시글 목록 요청: `GET /posts`
2. `PostController`가 요청을 받음
3. `PostService`의 비즈니스 로직 호출
4. `PostRepository`에서 데이터베이스 조회
5. 결과를 `Model`에 담아 `post-list.html` 반환
6. Thymeleaf가 HTML 렌더링

---

## Configuration

### Environment Profiles

프로젝트는 개발(dev)과 운영(prod) 환경을 분리하여 관리합니다:

- `application.properties` - 공통 설정
- `application-dev.properties` - 개발 환경 (H2 데이터베이스)
- `application-prod.properties` - 운영 환경 (MySQL/MariaDB)

### 주요 설정 (운영 환경)

```properties
# 서버 포트
server.port=8090

# 데이터베이스 (MySQL/MariaDB)
spring.datasource.url=jdbc:mysql://localhost:3306/starlog
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 설정
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# 파일 업로드
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload-dir=/home/kalpha/starlog/upload/

# 세션 타임아웃
server.servlet.session.timeout=60m

# 쿠키 보안
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict
server.servlet.session.cookie.http-only=true
```

---

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auth/login` | 로그인 페이지 |
| POST | `/auth/login` | 로그인 처리 |
| GET | `/auth/register` | 회원가입 페이지 |
| POST | `/auth/register` | 회원가입 처리 |
| POST | `/auth/logout` | 로그아웃 |
| GET | `/auth/delete-account` | 회원 탈퇴 페이지 |
| POST | `/auth/delete-account` | 회원 탈퇴 처리 |

### Posts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/posts` | 게시글 목록 (검색, 정렬, 페이지네이션) |
| GET | `/posts/{id}` | 게시글 상세 |
| GET | `/posts/write` | 게시글 작성 페이지 |
| POST | `/posts/write` | 게시글 작성 (이미지 첨부 포함) |
| GET | `/posts/{id}/edit` | 게시글 수정 페이지 |
| POST | `/posts/{id}/edit` | 게시글 수정 (이미지 추가/삭제 포함) |
| POST | `/posts/{id}/delete` | 게시글 삭제 |
| POST | `/api/{postId}/like-hate` | 좋아요/싫어요 |
| GET | `/posts/api/new?since={datetime}` | 실시간 새 게시글 조회 (5초 폴링) |
| GET | `/posts/api/best` | 인기 게시글 조회 (30초 폴링) |

### User / Mypage

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/home` | 홈페이지 |
| GET | `/mypage` | 마이페이지 |
| POST | `/mypage/bio` | 자기소개 수정 |
| POST | `/mypage/profile-image` | 프로필 이미지 변경 |

### Others

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/chatai` | AI 챗봇 |

---

## Database

### Production Database (MySQL/MariaDB)

운영 환경에서는 MySQL 또는 MariaDB를 사용합니다:

- **JDBC URL**: `jdbc:mysql://localhost:3306/starlog`
- **Database**: `starlog`
- **Username**: `starlog_user` (환경 변수로 설정)
- **Password**: (환경 변수로 설정)
- **Character Set**: `utf8mb4`
- **Collation**: `utf8mb4_unicode_ci`

### Development Database (H2)

개발 및 테스트 단계에서는 H2 파일 기반 데이터베이스를 사용합니다:

- **JDBC URL**: `jdbc:h2:file:./data/starlog;MODE=MySQL`
- **Username**: `sa`
- **Password**: (비어있음)
- **H2 Console**: http://localhost:8090/h2-console (dev 환경에서만 활성화)

### Entity Relationships

```
User (1) ←→ (N) Post
Post (1) ←→ (N) Comment
Post (1) ←→ (N) PostImage
Post (1) ←→ (N) Lh (Like/Hate)
User (1) ←→ (N) Comment
User (1) ←→ (N) Lh (Like/Hate)
```

### Tables

- `users` - 사용자 정보
- `posts` - 게시글
- `comments` - 댓글
- `post_image` - 게시글 이미지
- `lh` - 좋아요/싫어요 기록

### Schema Auto-Generation

`spring.jpa.hibernate.ddl-auto=update` 설정으로 엔티티 기반 스키마가 자동 생성됩니다.

---

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java          # 애플리케이션 진입점
│   │   ├── config/
│   │   │   ├── SecurityConfig.java       # 보안 설정
│   │   │   └── WebConfig.java            # 웹 설정
│   │   ├── controller/                   # HTTP 요청 처리
│   │   ├── service/                      # 비즈니스 로직
│   │   ├── repository/                   # 데이터 접근
│   │   ├── entity/                       # JPA 엔티티
│   │   ├── dto/                          # 데이터 전송 객체
│   │   ├── exception/                    # 예외 처리
│   │   └── util/
│   │       └── HangulUtils.java          # 한글 검색 유틸
│   └── resources/
│       ├── application.properties        # 공통 설정
│       ├── application-dev.properties    # 개발 환경
│       ├── application-prod.properties   # 운영 환경
│       ├── static/                       # 정적 리소스
│       └── templates/                    # Thymeleaf 템플릿
└── test/                                 # 테스트 코드
```

### Adding New Features

기능 추가 시 권장 순서:

1. **Entity 작성**: 도메인 모델 정의 (`entity/`)
2. **Repository 작성**: DB 접근 메서드 정의 (`repository/`)
3. **Service 작성**: 비즈니스 로직 구현 (`service/`)
4. **Controller 작성**: HTTP 엔드포인트 구현 (`controller/`)
5. **View 작성**: 화면 템플릿 작성 (`templates/`)
6. **Test 작성**: 단위 테스트 및 통합 테스트

### Code Style

- 각 계층의 책임을 명확히 분리
- Service 계층에 `@Transactional` 적용
- Repository는 `JpaRepository` 상속
- Controller에서 세션 관리 (`HttpSession`)
- Lombok 어노테이션 활용 (`@Getter`, `@Setter`, `@Builder` 등)

### Testing

```bash
# 전체 테스트 실행
./mvnw test

# 특정 테스트 클래스 실행
./mvnw test -Dtest=PostServiceTest

# 테스트 건너뛰고 빌드
./mvnw package -DskipTests
```

---

## Security

### Implemented Security Features

- **인증**: 세션 기반 사용자 인증 (`HttpSession`)
- **비밀번호 암호화**: BCrypt 해싱
- **XSS 방지**: Jsoup 1.18.3을 사용한 HTML 태그 제거
- **파일 업로드 보안**:
  - 허용된 확장자만 업로드 (jpg, png, gif, webp)
  - MIME 타입 검증
  - 파일 크기 제한 (10MB)
  - UUID 기반 안전한 파일명 생성
- **입력 검증**:
  - 사용자명: 3-20자
  - 비밀번호: 8자 이상
  - 이메일: 정규식 검증
- **SQL Injection 방지**: JPA Prepared Statement 사용
- **세션 보안**: 
  - 세션 타임아웃 설정
  - HTTPS 사용 시 Secure Cookie
  - SameSite=Strict 설정

### Security Best Practices

1. 민감한 정보는 환경 변수로 관리 (`DB_USERNAME`, `DB_PASSWORD`)
2. 운영 환경에서 H2 콘솔 비활성화
3. SQL 로그 비활성화 (`spring.jpa.show-sql=false`)
4. 정기적인 의존성 업데이트
5. `.gitignore`를 통한 설정 파일 보호

---

## Deployment

### systemd 서비스로 배포

1. JAR 파일 빌드

```bash
./mvnw clean package -DskipTests
```

2. systemd 서비스 파일 생성

`/etc/systemd/system/starlog.service` 파일을 생성하고 다음 내용을 추가:

```ini
[Unit]
Description=STARLOG Spring Boot Application
After=syslog.target network.target mariadb.service

[Service]
User=your_username
WorkingDirectory=/path/to/project
ExecStart=/usr/bin/java -jar /path/to/project/target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
Environment="DB_USERNAME=starlog_user"
Environment="DB_PASSWORD=your_password"
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

3. 서비스 활성화 및 시작

```bash
sudo systemctl daemon-reload
sudo systemctl enable starlog.service
sudo systemctl start starlog.service
sudo systemctl status starlog.service
```

### Cloudflare Tunnel 설정

외부 접속을 위한 Cloudflare Tunnel 설정:

```bash
# cloudflared 설치
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
sudo dpkg -i cloudflared-linux-amd64.deb

# Cloudflare 인증
cloudflared tunnel login

# Named Tunnel 생성
cloudflared tunnel create starlog

# DNS 레코드 추가
cloudflared tunnel route dns starlog your-domain.com

# systemd 서비스로 실행
sudo systemctl enable cloudflared.service
sudo systemctl start cloudflared.service
```

### 서비스 관리

자세한 내용은 [SERVICE_MANAGEMENT.md](SERVICE_MANAGEMENT.md)를 참조하세요.

---

## Troubleshooting

### 게시글 작성 오류

**증상**: "게시글 작성 중 오류가 발생했습니다"

**원인**: 업로드 디렉토리가 존재하지 않거나 권한 부족

**해결**:
```bash
mkdir -p /home/kalpha/starlog/upload
chmod 755 /home/kalpha/starlog/upload
```

### 데이터베이스 연결 오류

**증상**: Connection refused 또는 Access denied

**해결**:
```bash
# MySQL 서비스 확인
sudo systemctl status mariadb

# 사용자 및 권한 확인
sudo mysql
SHOW GRANTS FOR 'starlog_user'@'localhost';

# 환경 변수 확인
echo $DB_USERNAME
echo $DB_PASSWORD
```

### 로그 확인

```bash
# systemd 서비스 로그
sudo journalctl -u starlog.service -f

# 최근 에러만 확인
sudo journalctl -u starlog.service --since "5 minutes ago" | grep -i error

# 로그 파일 직접 확인
tail -f /var/log/syslog | grep starlog
```

---

## Related Documentation

프로젝트의 상세한 설정 및 운영 가이드:

- [SERVICE_MANAGEMENT.md](SERVICE_MANAGEMENT.md) - systemd 서비스 관리 가이드
- [SECURITY_GUIDE.md](SECURITY_GUIDE.md) - 보안 설정 및 민감 정보 관리
- [HANGUL_SEARCH_GUIDE.md](HANGUL_SEARCH_GUIDE.md) - 한글 검색 기능 사용법
- [MYSQL_SETUP_GUIDE.md](MYSQL_SETUP_GUIDE.md) - MySQL/MariaDB 설정 가이드

---

## Project Status

### Version: 1.3.0

### Completed Features

- 사용자 인증 (회원가입, 로그인, 로그아웃)
- **회원 탈퇴** (비밀번호 재확인, 연관 데이터 전체 삭제)
- 게시판 CRUD (생성, 조회, 수정, 삭제)
- **게시글 이미지 수정** (기존 이미지 선택 삭제 + 신규 이미지 추가)
- 한글 검색 (초성, 자모, 영타 변환)
- 댓글 시스템 (대댓글, 프로필 이미지 표시, GIF 지원)
- 파일 업로드 (이미지, Path Traversal 방지)
- 좋아요/싫어요 기능
- 마이페이지 (프로필 이미지, 자기소개)
- MySQL/MariaDB 마이그레이션
- Cloudflare Tunnel 배포
- systemd 서비스 자동 시작
- 실시간 새 게시글 폴림 (5초)
- **실시간 인기 게시글 갱신** (30초 폴림)
- **GSAP 애니메이션** (커뮤니티 페이지 진입, 동적 삽입 행 포함)
- **정적 리소스 브라우저 캐싱** (30일 Cache-Control)
- **업로드 파일 캐시 제어 및 정적 리소스 핸들러 개선**
- **Spline 3D 인터랙티브 배경** (홈페이지)
- **모바일 반응형 개선** (애니메이션 분리, 스크롤 동작 개선, 가시성 향상)
- **CSS 최적화** (overflow 속성 개선, 스크롤 동작 개선)
- **Cloudflare Turnstile** — 로그인/회원가입 봇 방지 및 자동화 공격 차단
- 서버사이드 페이지네이션
- 사이트 파비콘 (둥근 로고)

### Test Coverage

- 28 unit tests passing
- PostServiceTest: 10 tests
- HangulUtilsTest: 10 tests
- UserServiceTest: 7 tests
- Integration tests: 1 test

---

## Contributing

이 프로젝트에 기여하고 싶으시다면:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License.

---

## Contact

**Team**: 미래유망팀  
**Repository**: [mirae-yumang-team](https://github.com/mirae-yumang-team-project/mirae-yumang-team)  
**Live Site**: https://starlog.c01.kr

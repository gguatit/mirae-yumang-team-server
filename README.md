# STARLOG

> 학교 커뮤니티 기반 웹 애플리케이션

미래유망팀의 Spring Boot 기반 통합 커뮤니티 플랫폼입니다. 게시판, 채용공고, AI 챗봇 등 다양한 기능을 제공합니다.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

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

STARLOG는 학교 커뮤니티를 위한 종합 웹 플랫폼으로, 게시글 작성/조회, 사용자 인증, 파일 업로드, 채용공고 확인, AI 챗봇 등의 기능을 제공합니다. Spring Boot의 MVC 아키텍처와 계층형 구조를 기반으로 설계되었습니다.

### Key Highlights

- 완전한 CRUD 기능을 갖춘 게시판 시스템
- 세션 기반 사용자 인증 및 권한 관리
- 이미지 업로드 및 파일 관리
- 반응형 UI (Thymeleaf)
- H2 인메모리 데이터베이스 사용

---

## Features

- **사용자 인증**: 회원가입, 로그인, 로그아웃, 세션 관리
- **게시판**: 게시글 작성, 수정, 삭제, 조회수, 좋아요/싫어요, 검색 및 정렬
- **댓글 시스템**: 게시글별 댓글 작성 및 관리
- **파일 업로드**: 이미지 업로드 및 저장 (최대 10MB)
- **채용공고**: 채용 정보 조회
- **AI 챗봇**: 챗봇 인터페이스
- **마이페이지**: 사용자 프로필 및 작성글 관리
- **홈페이지**: 메인 대시보드

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| **Backend** | Spring Boot 3.5.7, Java 17 |
| **Web Framework** | Spring MVC |
| **Template Engine** | Thymeleaf |
| **ORM** | Spring Data JPA, Hibernate |
| **Database** | H2 (In-Memory) |
| **Build Tool** | Maven |
| **Server** | Tomcat (Embedded) |
| **External Libraries** | Jsoup 1.15.3, Lombok |

---

## Getting Started

### Prerequisites

다음 소프트웨어가 설치되어 있어야 합니다:

- **Java 17** or higher
- **Maven 3.6+** (또는 포함된 Maven Wrapper 사용)
- **Git**

### Installation

1. 저장소 클론

```bash
git clone https://github.com/mirae-yumang-team-project/mirae-yumang-team.git
cd mirae-yumang-team
```

2. 의존성 설치

```bash
./mvnw clean install
```

### Running the Application

#### 옵션 1: Maven Wrapper 사용 (권장)

```bash
./mvnw spring-boot:run
```

#### 옵션 2: JAR 파일 빌드 후 실행

```bash
./mvnw package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

#### 옵션 3: 백그라운드 실행 (nohup)

```bash
nohup ./mvnw spring-boot:run > ~/spring-boot.log 2>&1 &
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
│   │   │   └── WebConfig.java            # 웹 설정 (파일 업로드 경로 등)
│   │   ├── controller/                   # HTTP 요청 처리
│   │   │   ├── HomeController.java
│   │   │   ├── AuthController.java
│   │   │   ├── PostController.java
│   │   │   ├── CommentController.java
│   │   │   ├── AnnouncementController.java
│   │   │   ├── ChatBotAiController.java
│   │   │   └── ...
│   │   ├── service/                      # 비즈니스 로직
│   │   │   ├── UserService.java
│   │   │   ├── PostService.java
│   │   │   └── ...
│   │   ├── repository/                   # 데이터 접근 계층
│   │   │   ├── UserRepository.java
│   │   │   ├── PostRepository.java
│   │   │   └── ...
│   │   ├── entity/                       # JPA 엔티티
│   │   │   ├── User.java
│   │   │   ├── Post.java
│   │   │   └── ...
│   │   ├── dto/                          # 데이터 전송 객체
│   │   └── crawler/                      # 크롤링 유틸리티
│   └── resources/
│       ├── application.properties        # 애플리케이션 설정
│       ├── static/                       # 정적 리소스
│       │   ├── css/
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

### application.properties

주요 설정 항목:

```properties
# 서버 포트
server.port=8090

# 데이터베이스
spring.datasource.url=jdbc:h2:~/testdb
spring.datasource.username=sa
spring.datasource.password=

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# H2 콘솔 (프로덕션에서는 비활성화)
spring.h2.console.enabled=false

# 파일 업로드
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
file.upload-dir=/home/kalpha/starlog/upload/

# 세션 타임아웃
server.servlet.session.timeout=30m
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
| GET | `/auth/logout` | 로그아웃 |

### Posts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/posts` | 게시글 목록 (검색, 정렬) |
| GET | `/posts/{id}` | 게시글 상세 |
| GET | `/posts/write` | 게시글 작성 페이지 |
| POST | `/posts/write` | 게시글 작성 |
| GET | `/posts/{id}/edit` | 게시글 수정 페이지 |
| POST | `/posts/{id}/edit` | 게시글 수정 |
| POST | `/posts/{id}/delete` | 게시글 삭제 |
| POST | `/api/{postId}/like-hate` | 좋아요/싫어요 |

### Others

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/home` | 홈페이지 |
| GET | `/mypage` | 마이페이지 |
| GET | `/announcement` | 채용공고 |
| GET | `/chatai` | AI 챗봇 |

---

## Database

### H2 Database

개발 단계에서는 H2 인메모리 데이터베이스를 사용합니다:

- **JDBC URL**: `jdbc:h2:~/testdb`
- **Username**: `sa`
- **Password**: (empty)

### Entity Relationships

```
User (1) ←→ (N) Post
Post (1) ←→ (N) Comment
```

### Schema Auto-Generation

`spring.jpa.hibernate.ddl-auto=update` 설정으로 엔티티 기반 스키마가 자동 생성됩니다.

---

## Development

### Adding New Features

기능 추가 시 권장 순서:

1. **Entity 작성**: 도메인 모델 정의 (`entity/`)
2. **Repository 작성**: DB 접근 메서드 정의 (`repository/`)
3. **Service 작성**: 비즈니스 로직 구현 (`service/`)
4. **Controller 작성**: HTTP 엔드포인트 구현 (`controller/`)
5. **View 작성**: 화면 템플릿 작성 (`templates/`)

### Code Style

- 각 계층의 책임을 명확히 분리
- Service 계층에 `@Transactional` 적용
- Repository는 `JpaRepository` 상속
- Controller에서 세션 관리 (`HttpSession`)

### Testing

```bash
./mvnw test
```

---

## Security

### Current Implementation

- 세션 기반 인증 (`HttpSession`)
- 비밀번호 평문 저장 (개발 환경)

### Recommended Improvements

- **Spring Security 도입**: 인증/인가 강화
- **비밀번호 암호화**: BCrypt 해싱
- **CSRF 보호**: Spring Security CSRF 토큰
- **입력 검증**: Bean Validation (JSR-380)
- **H2 콘솔 비활성화**: 프로덕션 환경에서 필수

---

## Troubleshooting

### 포트 충돌

```bash
# 8090 포트를 사용 중인 프로세스 확인
lsof -i :8090

# 프로세스 종료
kill -9 <PID>
```

### 서버 로그 확인

```bash
# 실시간 로그 모니터링
tail -f ~/spring-boot.log

# 최근 100줄 확인
tail -100 ~/spring-boot.log
```

### 서버 프로세스 관리

```bash
# 실행 중인 서버 확인
ps aux | grep spring-boot

# 서버 중지
pkill -f "spring-boot:run"

# 서버 재시작
pkill -f "spring-boot:run" && nohup ./mvnw spring-boot:run > ~/spring-boot.log 2>&1 &
```

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

# MySQL 설정 가이드

##  개요

이 프로젝트는 이제 **개발 환경에서는 H2**, **운영 환경에서는 MySQL**을 사용합니다.

---

##  빠른 시작

### 방법 1: 로컬 MySQL 설치 (권장)

#### Linux (Ubuntu/Debian)

```bash
# MySQL 설치
sudo apt update
sudo apt install mysql-server

# MySQL 보안 설정
sudo mysql_secure_installation

# MySQL 서비스 시작
sudo systemctl start mysql
sudo systemctl enable mysql

# MySQL 접속
sudo mysql -u root -p
```

#### macOS (Homebrew)

```bash
# MySQL 설치
brew install mysql

# MySQL 시작
brew services start mysql

# MySQL 접속
mysql -u root -p
```

#### Windows

1. [MySQL 공식 사이트](https://dev.mysql.com/downloads/mysql/) 다운로드
2. MySQL Installer 실행
3. MySQL Server 8.0 설치
4. MySQL Workbench로 접속

---

### 방법 2: Docker 사용 (가장 쉬움)

```bash
# MySQL 8.0 컨테이너 실행
docker run --name starlog-mysql \
  -e MYSQL_ROOT_PASSWORD=password123 \
  -e MYSQL_DATABASE=starlog \
  -e MYSQL_USER=starlog_user \
  -e MYSQL_PASSWORD=starlog_pass \
  -p 3306:3306 \
  -d mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

# 컨테이너 상태 확인
docker ps

# 컨테이너 중지
docker stop starlog-mysql

# 컨테이너 시작
docker start starlog-mysql

# 컨테이너 삭제 (데이터도 삭제됨)
docker rm -f starlog-mysql
```

---

## ️ 데이터베이스 생성

### 1. MySQL 접속

```bash
# 로컬 MySQL
mysql -u root -p

# Docker MySQL
docker exec -it starlog-mysql mysql -u root -p
```

### 2. 데이터베이스 생성

```sql
-- 데이터베이스 생성 (한글 지원을 위해 utf8mb4 사용)
CREATE DATABASE starlog 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 전용 사용자 생성 (선택사항, 보안 강화)
CREATE USER 'starlog_user'@'localhost' IDENTIFIED BY 'starlog_password123';
GRANT ALL PRIVILEGES ON starlog.* TO 'starlog_user'@'localhost';
FLUSH PRIVILEGES;

-- 확인
SHOW DATABASES;
USE starlog;
```

### 3. 연결 테스트

```bash
# 생성한 데이터베이스로 접속
mysql -u starlog_user -p starlog
```

---

##  애플리케이션 설정

### 옵션 1: 환경 변수 사용 (권장)

```bash
# Linux/macOS
export DB_USERNAME=starlog_user
export DB_PASSWORD=starlog_password123

# Windows (PowerShell)
$env:DB_USERNAME="starlog_user"
$env:DB_PASSWORD="starlog_password123"

# Windows (CMD)
set DB_USERNAME=starlog_user
set DB_PASSWORD=starlog_password123
```

### 옵션 2: application-prod.properties 직접 수정

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/starlog?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=starlog_user
spring.datasource.password=starlog_password123
```

---

##  애플리케이션 실행

### 개발 모드 (H2 사용)

```bash
# H2 인메모리 DB 사용
./mvnw spring-boot:run

# 또는 dev 프로필 명시
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 운영 모드 (MySQL 사용)

```bash
# 환경 변수 설정 후
export DB_USERNAME=starlog_user
export DB_PASSWORD=starlog_password123

# MySQL 사용하여 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# 또는 JAR 실행
./mvnw clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 첫 실행 (테이블 자동 생성)

**초기 실행 시** `ddl-auto=update`로 변경:

`application-prod.properties`:
```properties
spring.jpa.hibernate.ddl-auto=update  # 첫 실행
```

실행 후 테이블이 생성되면 다시 변경:
```properties
spring.jpa.hibernate.ddl-auto=validate  # 이후 실행
```

---

##  연결 확인

### 1. 애플리케이션 로그 확인

```
...
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
...
```

### 2. MySQL에서 테이블 확인

```sql
USE starlog;

-- 테이블 목록 확인
SHOW TABLES;

-- 예상 테이블
-- +------------------+
-- | Tables_in_starlog|
-- +------------------+
-- | comments         |
-- | lh               |
-- | post_images      |
-- | posts            |
-- | users            |
-- +------------------+

-- 테이블 구조 확인
DESCRIBE posts;
DESCRIBE users;

-- 데이터 확인
SELECT * FROM users;
SELECT * FROM posts;
```

---

##  문제 해결

### 1. "Access denied for user" 오류

```bash
# MySQL 접속 확인
mysql -u starlog_user -p starlog

# 권한 재부여
mysql -u root -p
GRANT ALL PRIVILEGES ON starlog.* TO 'starlog_user'@'localhost';
FLUSH PRIVILEGES;
```

### 2. "Unknown database 'starlog'" 오류

```sql
-- 데이터베이스 생성 확인
SHOW DATABASES;

-- 없으면 생성
CREATE DATABASE starlog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. "Communications link failure" 오류

```bash
# MySQL 서비스 상태 확인
sudo systemctl status mysql  # Linux
brew services list           # macOS
docker ps                    # Docker

# MySQL 재시작
sudo systemctl restart mysql  # Linux
brew services restart mysql   # macOS
docker restart starlog-mysql  # Docker
```

### 4. "Public Key Retrieval is not allowed" 오류

URL에 `allowPublicKeyRetrieval=true` 추가:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/starlog?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Seoul
```

### 5. 한글 깨짐

```sql
-- 데이터베이스 인코딩 확인
SHOW VARIABLES LIKE 'character%';

-- utf8mb4가 아니면 재생성
DROP DATABASE starlog;
CREATE DATABASE starlog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

##  MySQL 관리 도구

### 1. MySQL Workbench (GUI)
- [다운로드](https://dev.mysql.com/downloads/workbench/)
- 시각적 데이터베이스 관리

### 2. DBeaver (무료, 다중 DB 지원)
- [다운로드](https://dbeaver.io/)
- 무료 오픈소스

### 3. phpMyAdmin (웹 기반)
```bash
# Docker로 phpMyAdmin 실행
docker run --name phpmyadmin \
  --link starlog-mysql:db \
  -p 8080:80 \
  -d phpmyadmin/phpmyadmin
```

접속: http://localhost:8080

---

##  보안 권장사항

1. **강력한 비밀번호 사용**
   ```bash
   # 복잡한 비밀번호 생성
   openssl rand -base64 32
   ```

2. **환경 변수로 비밀번호 관리**
   - `.env` 파일 사용 (`.gitignore`에 추가)
   - 운영 서버에서는 시스템 환경 변수

3. **최소 권한 원칙**
   ```sql
   -- 필요한 권한만 부여
   GRANT SELECT, INSERT, UPDATE, DELETE ON starlog.* TO 'starlog_user'@'localhost';
   ```

4. **외부 접근 제한**
   - `'starlog_user'@'localhost'` - 로컬만 접근 가능
   - 원격 접근 필요 시: `'starlog_user'@'%'` (주의!)

---

##  성능 최적화

### 1. 연결 풀 설정

`application-prod.properties`:
```properties
# HikariCP 설정
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### 2. 인덱스 추가

```sql
-- 자주 검색하는 컬럼에 인덱스
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_users_username ON users(username);
```

### 3. MySQL 설정 최적화

`/etc/mysql/my.cnf` (또는 `/etc/my.cnf`):
```ini
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 1G
query_cache_size = 64M
```

---

##  H2에서 MySQL로 데이터 마이그레이션

### 1. H2 데이터 내보내기

```sql
-- H2 Console (http://localhost:8090/h2-console)
SCRIPT TO 'data.sql';
```

### 2. MySQL 형식으로 변환

```bash
# H2 특화 문법 제거/변경
sed -i 's/IDENTITY/AUTO_INCREMENT/g' data.sql
```

### 3. MySQL에 임포트

```bash
mysql -u starlog_user -p starlog < data.sql
```

---

##  참고 자료

- [MySQL 공식 문서](https://dev.mysql.com/doc/)
- [Spring Boot Data Access](https://spring.io/guides/gs/accessing-data-mysql/)
- [Hibernate MySQL Dialect](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#database-dialect)

---

##  체크리스트

실행 전 확인:

- [ ] MySQL 설치 및 실행 중
- [ ] `starlog` 데이터베이스 생성
- [ ] MySQL 사용자 생성 및 권한 부여
- [ ] 환경 변수 설정 또는 `application-prod.properties` 수정
- [ ] 첫 실행 시 `ddl-auto=update` 설정
- [ ] 애플리케이션 실행: `--spring.profiles.active=prod`
- [ ] 브라우저에서 접속: http://localhost:8090
- [ ] MySQL에서 테이블 생성 확인
- [ ] 회원가입/로그인 테스트
- [ ] 게시글 작성 테스트

---

문제가 발생하면 로그를 확인하고, 이 가이드의 "문제 해결" 섹션을 참고하세요.

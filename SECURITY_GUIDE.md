#  보안 가이드

## ️ 중요: Git에 절대 커밋하면 안 되는 파일들

다음 파일들은 **민감한 정보**가 포함되어 있어 Git에 커밋되지 않도록 `.gitignore`에 추가되어 있습니다:

### 1. 설정 파일
- `src/main/resources/application-prod.properties` - 운영 환경 DB 정보
- `src/main/resources/application-dev.properties` - 개발 환경 설정
- `src/main/resources/application-local.properties` - 로컬 설정
- `.env`, `.env.local`, `.env.prod` - 환경 변수 파일

### 2. systemd 서비스 파일
- `*.service` - DB 비밀번호 등이 포함된 서비스 파일
- `starlog.service` 
- `cloudflared.service`

### 3. Cloudflare 인증 정보
- `.cloudflared/` 디렉토리 전체
  - `cert.pem` - Cloudflare 인증서
  - `*.json` - Tunnel credentials
  - `config.yml` - Tunnel 설정

### 4. 파일 및 데이터베이스
- `upload/` - 업로드된 파일들
- `starlog/` - 애플리케이션 데이터
- `*.sql`, `*.sql.gz` - 데이터베이스 백업
- `*.db` - 데이터베이스 파일

### 5. 빌드 산출물
- `*.jar` - 빌드된 JAR 파일
- `*.log` - 로그 파일
- `target/` - Maven 빌드 디렉토리

---

##  안전한 설정 방법

### 1단계: 템플릿 파일 복사

프로젝트에는 안전한 **템플릿 파일**이 포함되어 있습니다:

```bash
# 설정 파일 템플릿 복사
cp src/main/resources/application-prod.properties.template \
   src/main/resources/application-prod.properties

cp src/main/resources/application-dev.properties.template \
   src/main/resources/application-dev.properties

# systemd 서비스 템플릿 복사
cp starlog.service.template starlog.service
cp cloudflared.service.template cloudflared.service
```

### 2단계: 실제 값으로 변경

복사한 파일들을 열어서 다음 정보를 **실제 값**으로 변경하세요:

#### application-prod.properties
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/YOUR_DATABASE_NAME
spring.datasource.username=${DB_USERNAME:YOUR_DB_USERNAME}
spring.datasource.password=${DB_PASSWORD:YOUR_DB_PASSWORD}
file.upload-dir=/var/www/YOUR_APP_NAME/upload/
```

#### starlog.service
```ini
User=YOUR_USERNAME
WorkingDirectory=/path/to/your/project
ExecStart=/usr/bin/java -jar /path/to/your/project/target/demo-0.0.1-SNAPSHOT.jar
Environment="DB_USERNAME=YOUR_DB_USERNAME"
Environment="DB_PASSWORD=YOUR_DB_PASSWORD"
```

### 3단계: 환경 변수 사용 (권장)

서비스 파일에 직접 비밀번호를 쓰는 대신, 환경 변수를 사용하세요:

```bash
# /etc/environment 또는 systemd override 사용
sudo systemctl edit starlog.service
```

다음 내용 추가:
```ini
[Service]
Environment="DB_USERNAME=your_username"
Environment="DB_PASSWORD=your_secure_password"
```

---

##  커밋 전 확인사항

Git에 커밋하기 전에 **반드시** 확인하세요:

```bash
# 1. 민감한 파일이 추적되는지 확인
git status

# 2. .gitignore가 제대로 작동하는지 확인
git check-ignore -v *.service
git check-ignore -v src/main/resources/application-prod.properties

# 3. 커밋할 파일 목록 확인
git diff --cached --name-only
```

---

##  실수로 커밋한 경우

### 아직 push하지 않았다면:

```bash
# 마지막 커밋 취소 (변경사항은 유지)
git reset --soft HEAD~1

# 파일을 staging에서 제거
git reset HEAD sensitive-file.properties

# .gitignore에 추가하고 다시 커밋
echo "sensitive-file.properties" >> .gitignore
git add .gitignore
git commit -m "Add sensitive file to .gitignore"
```

### 이미 push했다면:

️ **즉시 조치 필요!**

1. **비밀번호 즉시 변경**: 노출된 DB 비밀번호, API 키 등을 즉시 변경
2. **Git history에서 완전히 제거**:

```bash
# BFG Repo-Cleaner 사용 (권장)
# https://rtyley.github.io/bfg-repo-cleaner/
bfg --delete-files application-prod.properties
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# 강제 push
git push origin --force --all
```

3. **GitHub에서 확인**: Repository → Settings → Secrets scanning alerts

---

##  권장 사항

### 1. 환경 변수 사용

비밀번호를 파일에 저장하지 말고 환경 변수로 관리:

```bash
# ~/.bashrc 또는 ~/.profile에 추가
export DB_USERNAME=your_username
export DB_PASSWORD=your_secure_password
```

### 2. 별도의 secrets 관리

민감한 정보는 별도 파일로 관리하고 `.gitignore`에 추가:

```bash
# secrets.env 파일 생성 (Git에 추가하지 않음)
echo "secrets.env" >> .gitignore
```

### 3. GitHub Secrets 사용

CI/CD 사용 시 GitHub Secrets에 저장:
- Repository → Settings → Secrets and variables → Actions → New repository secret

### 4. 정기적인 보안 점검

```bash
# Git history에서 민감한 정보 검색
git log -p | grep -i "password\|secret\|key"

# 현재 추적 중인 파일 확인
git ls-files | grep -E "\.properties$|\.service$"
```

---

##  추가 보안 조치

### 파일 권한 설정

```bash
# 설정 파일 권한 제한 (소유자만 읽기/쓰기)
chmod 600 src/main/resources/application-prod.properties
chmod 600 .env

# systemd 서비스 파일
sudo chmod 644 /etc/systemd/system/starlog.service
```

### 방화벽 설정

```bash
# 8090 포트는 localhost만 접근 (Cloudflare Tunnel 사용 시)
sudo ufw allow from 127.0.0.1 to any port 8090

# 외부 접근은 Cloudflare를 통해서만
sudo ufw allow 443/tcp
sudo ufw enable
```

---

##  안전한 협업 가이드

팀원들과 협업할 때:

1. **템플릿 파일만 공유**: `.template` 파일만 Git에 커밋
2. **문서화**: README에 설정 방법 명시
3. **코드 리뷰**: PR에서 민감한 정보가 없는지 확인
4. **자동화**: pre-commit hook으로 민감한 파일 체크

### Pre-commit hook 설정 예시

`.git/hooks/pre-commit` 파일 생성:

```bash
#!/bin/bash
# 민감한 파일이 커밋되는지 확인

SENSITIVE_FILES="application-prod.properties application-dev.properties *.service"

for pattern in $SENSITIVE_FILES; do
    if git diff --cached --name-only | grep -q "$pattern"; then
        echo " Error: 민감한 파일이 포함되어 있습니다: $pattern"
        echo "   .gitignore를 확인하세요!"
        exit 1
    fi
done

exit 0
```

```bash
chmod +x .git/hooks/pre-commit
```

---

** 보안은 한 번의 실수로 무너질 수 있습니다. 항상 주의하세요!**

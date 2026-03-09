# STARLOG 서비스 관리 가이드

##  배포 완료 정보

###  접속 URL
- **공개 도메인**: https://starlog.c01.kr
- **로컬 접속**: http://localhost:8090

###  systemd 서비스
두 개의 서비스가 자동으로 시작되며 영구 실행됩니다:

1. **starlog.service** - Spring Boot 애플리케이션
2. **cloudflared.service** - Cloudflare Tunnel (starlog.c01.kr 연결)

---

##  서비스 관리 명령어

### 서비스 상태 확인
```bash
# 두 서비스 상태 확인
sudo systemctl status starlog.service
sudo systemctl status cloudflared.service

# 간단한 상태 확인
sudo systemctl is-active starlog.service cloudflared.service
```

### 서비스 시작/중지/재시작
```bash
# Spring Boot 서버
sudo systemctl start starlog.service      # 시작
sudo systemctl stop starlog.service       # 중지
sudo systemctl restart starlog.service    # 재시작

# Cloudflare Tunnel
sudo systemctl start cloudflared.service
sudo systemctl stop cloudflared.service
sudo systemctl restart cloudflared.service

# 두 서비스 동시 재시작
sudo systemctl restart starlog.service cloudflared.service
```

### 로그 확인
```bash
# 실시간 로그 보기 (Ctrl+C로 종료)
sudo journalctl -u starlog.service -f
sudo journalctl -u cloudflared.service -f

# 최근 로그 50줄 보기
sudo journalctl -u starlog.service -n 50 --no-pager
sudo journalctl -u cloudflared.service -n 50 --no-pager

# 오늘의 로그만 보기
sudo journalctl -u starlog.service --since today
```

### 자동 시작 설정
```bash
# 자동 시작 활성화 (이미 설정됨)
sudo systemctl enable starlog.service
sudo systemctl enable cloudflared.service

# 자동 시작 비활성화
sudo systemctl disable starlog.service
sudo systemctl disable cloudflared.service

# 자동 시작 여부 확인
sudo systemctl is-enabled starlog.service cloudflared.service
```

---

## ️ 데이터베이스 관리

### MySQL/MariaDB 접속
```bash
# root로 접속
sudo mysql

# starlog 데이터베이스 사용
sudo mysql starlog

# starlog_user로 접속
mysql -u starlog_user -p starlog
# 비밀번호: starlog123
```

### 데이터베이스 백업
```bash
# 전체 백업
sudo mysqldump starlog > ~/backup_$(date +%Y%m%d_%H%M%S).sql

# 압축 백업
sudo mysqldump starlog | gzip > ~/backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### 데이터베이스 복원
```bash
# 백업 파일 복원
sudo mysql starlog < ~/backup_20260309_013000.sql
```

---

##  Cloudflare Tunnel 관리

### 터널 정보 확인
```bash
# 터널 목록 보기
cloudflared tunnel list

# 터널 상세 정보
cloudflared tunnel info starlog

# 터널 라우팅 확인
cloudflared tunnel route ip show
```

### 설정 파일 위치
- **설정 파일**: `~/.cloudflared/config.yml`
- **인증 정보**: `~/.cloudflared/cert.pem`
- **터널 credentials**: `~/.cloudflared/4a0670f7-977a-4d19-94b0-6f23002c3dcd.json`

---

##  애플리케이션 업데이트

### JAR 파일 다시 빌드
```bash
cd ~/mirae-yumang-team-server

# 빌드 (테스트 포함)
./mvnw clean package

# 빌드 (테스트 제외)
./mvnw clean package -DskipTests

# 서비스 재시작
sudo systemctl restart starlog.service
```

### 환경 변수 변경
서비스 파일을 수정하려면:
```bash
# 서비스 파일 위치
sudo nano /etc/systemd/system/starlog.service

# 변경 후 적용
sudo systemctl daemon-reload
sudo systemctl restart starlog.service
```

---

##  문제 해결

### 서비스가 시작되지 않을 때
```bash
# 실패 이유 확인
sudo systemctl status starlog.service -l

# 최근 에러 로그 확인
sudo journalctl -u starlog.service -n 100 --no-pager | grep -i error

# JAR 파일 존재 확인
ls -lh ~/mirae-yumang-team-server/target/demo-0.0.1-SNAPSHOT.jar
```

### 데이터베이스 연결 오류
```bash
# MySQL 서비스 확인
sudo systemctl status mariadb

# MySQL 재시작
sudo systemctl restart mariadb

# 연결 테스트
mysql -u starlog_user -pstarlog123 starlog -e "SELECT 1;"
```

### 터널 연결 문제
```bash
# cloudflared 로그 확인
sudo journalctl -u cloudflared.service -n 50

# 터널 수동 실행 (디버깅용)
cloudflared tunnel run starlog

# DNS 확인
dig starlog.c01.kr
nslookup starlog.c01.kr
```

### 포트 8090이 사용 중일 때
```bash
# 포트를 사용하는 프로세스 확인
sudo lsof -i :8090

# 프로세스 종료
sudo kill -9 <PID>
```

---

##  모니터링

### 리소스 사용량 확인
```bash
# 서비스 메모리 사용량
sudo systemctl status starlog.service | grep Memory

# CPU 사용량
top -p $(pgrep -f demo-0.0.1-SNAPSHOT.jar)

# 전체 시스템 상태
htop
```

### 디스크 사용량
```bash
# 전체 디스크 사용량
df -h

# 프로젝트 디렉토리 크기
du -sh ~/mirae-yumang-team-server

# MySQL 데이터 크기
sudo du -sh /var/lib/mysql/starlog
```

---

##  서버 재부팅 후

서버를 재부팅해도 다음이 자동으로 시작됩니다:
1.  MariaDB 데이터베이스
2.  STARLOG Spring Boot 애플리케이션
3.  Cloudflare Tunnel

**아무 작업도 필요 없습니다!** 

확인 방법:
```bash
# 재부팅 후 모든 서비스 확인
sudo systemctl is-active mariadb starlog cloudflared
```

---

##  중요 파일 위치

- **JAR 파일**: `/home/kalpha/mirae-yumang-team-server/target/demo-0.0.1-SNAPSHOT.jar`
- **설정 파일**: `/home/kalpha/mirae-yumang-team-server/src/main/resources/application-prod.properties`
- **systemd 서비스**: `/etc/systemd/system/starlog.service`
- **Cloudflare 설정**: `~/.cloudflared/config.yml`
- **업로드 디렉토리**: `~/starlog/upload/`

---

##  보안 정보

- **데이터베이스**: starlog
- **DB 사용자**: starlog_user
- **DB 비밀번호**: starlog123 (️ 운영 환경에서는 더 강력한 비밀번호로 변경 권장)
- **서버 포트**: 8090 (localhost만 접근 가능)
- **공개 접속**: Cloudflare Tunnel을 통해서만 가능 (https://starlog.c01.kr)

---

** 모든 설정이 완료되었습니다!**

VSCode를 종료하거나 터미널을 닫아도 서비스는 계속 실행됩니다.

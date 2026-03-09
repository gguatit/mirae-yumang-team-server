#  한글 검색 기능 개선 완료

es-hangul 라이브러리의 기능을 Java로 구현하여 게시판 검색 기능을 대폭 개선했습니다.

##  새로운 기능

### 1. **초성 검색**
사용자가 초성만 입력해도 검색 가능합니다.

**예시:**
- `ㅅㅍㄹ` 검색 → "스프링", "스프레드" 등 검색됨
- `ㅈㅂ` 검색 → "자바", "자반", "점보" 등 검색됨
- `ㅎㄱㄱㅅ` 검색 → "한글검색" 검색됨

### 2. **자모 분리 검색**
한글을 자모 단위로 분리하여 검색합니다.

**예시:**
- `ㅎㅏㄴ` 검색 → "한글", "한국" 등 검색됨

### 3. **영타 자동 변환**
영문 키보드로 입력한 내용을 한글 자모로 자동 변환하여 검색합니다.

**예시:**
- `gks` 입력 → `ㅎㅏㄴ` 으로 변환되어 "한글" 검색됨
- `dkssud` 입력 → "안녕" 검색됨

### 4. **스마트 매칭**
여러 검색 방식을 동시에 적용하여 최적의 결과를 제공합니다.

### 5. **검색 점수 기반 정렬**
검색어와의 관련도를 점수화하여 더 정확한 결과를 우선 표시합니다.

---

##  사용 방법

### API 엔드포인트

#### 기본 검색 (기존 방식)
```
GET /posts?keyword=검색어
```

#### 향상된 검색 (초성/자모/영타 지원)
```
GET /posts?keyword=검색어&searchMode=enhanced
```

**파라미터:**
- `keyword`: 검색할 키워드
- `searchMode`: 검색 모드
  - `basic` (기본값): 일반 텍스트 검색만
  - `enhanced`: 초성/자모/영타 등 고급 검색

---

##  코드 사용 예시

### 1. Controller에서 사용

```java
@GetMapping
public String list(@RequestParam String keyword,
                   @RequestParam(defaultValue = "enhanced") String searchMode,
                   Model model) {
    Page<Post> posts;
    
    if ("enhanced".equals(searchMode)) {
        posts = postService.getPagedPostsEnhanced(keyword, 0);
    } else {
        posts = postService.getPagedPosts(keyword, 0);
    }
    
    model.addAttribute("posts", posts);
    return "post-list";
}
```

### 2. Service에서 직접 사용

```java
// 향상된 검색
List<Post> results = postService.searchPostsEnhanced("ㅅㅍㄹ");

// 또는
Page<Post> pagedResults = postService.getPagedPostsEnhanced("자바", 0);
```

### 3. HangulUtils 직접 사용

```java
// 초성 추출
String choseong = HangulUtils.getChoseong("안녕하세요");
// 결과: "ㅇㄴㅎㅅㅇ"

// 초성 매칭 확인
boolean matches = HangulUtils.matchesChoseong("스프링부트", "ㅅㅍㄹ");
// 결과: true

// 향상된 매칭 (모든 검색 방식 적용)
boolean found = HangulUtils.enhancedMatch("게시판 검색", "ㄱㅅㅂ");
// 결과: true

// 검색 점수 계산
int score = HangulUtils.calculateSearchScore("스프링", "스프");
// 결과: 50+ (시작 위치 일치)

// 영타를 한글로 변환
String hangul = HangulUtils.qwertyToHangul("gks");
// 결과: "ㅎㅏㄴ" (자모 형태)
```

---

##  실제 검색 시나리오

### 시나리오 1: 제목만 기억나는 경우
게시글 제목: "자바 스프링 강의"

가능한 검색어:
- `자바` 
- `ㅈㅂ`  (초성)
- `ㅅㅍㄹ`  (부분 초성)
- `wkqk`  (영타 실수)

### 시나리오 2: 키보드 설정 잘못된 경우
게시글 제목: "한글 처리 라이브러리"

영어 키보드로 `gksrmf`를 입력해도 찾아짐 

### 시나리오 3: 빠른 검색
게시글 제목: "알고리즘 문제 풀이"

초성만으로 빠르게 검색: `ㅇㄱㄹㅈ` 

---

##  성능 고려사항

### 기본 검색 (searchMode=basic)
- **장점**: DB 레벨 검색으로 빠름
- **단점**: 정확한 텍스트 매칭만 가능
- **권장**: 게시글이 많을 때 (1000개 이상)

### 향상된 검색 (searchMode=enhanced)
- **장점**: 초성/자모/영타 등 다양한 검색 지원
- **단점**: 메모리에서 필터링하므로 데이터가 많으면 느릴 수 있음
- **권장**: 게시글이 적을 때 (1000개 미만) 또는 고급 검색 필요 시

---

##  테스트

모든 기능은 단위 테스트로 검증되었습니다:

```bash
./mvnw test -Dtest=HangulUtilsTest
```

**테스트 커버리지:**
-  한글 문자 판별
-  초성 추출
-  자모 분리
-  초성 매칭
-  영타 변환
-  향상된 매칭
-  검색 점수 계산
-  실제 시나리오 테스트

---

##  참고

이 구현은 toss의 [es-hangul](https://github.com/toss/es-hangul) 라이브러리에서 영감을 받았습니다.

### es-hangul 주요 기능 구현 현황

| 기능 | es-hangul | 현재 구현 | 상태 |
|-----|-----------|----------|------|
| 초성 추출 (`getChoseong`) |  |  | 완료 |
| 자모 분리 (`disassemble`) |  |  | 완료 |
| 초성 매칭 |  |  | 완료 |
| 조사 처리 (`josa`) |  |  | 미구현 |
| 자모 조합 (`assemble`) |  |  | 미구현 |
| 표준 발음 |  |  | 미구현 |

---

##  향후 개선 사항

1. **인덱싱 최적화**
   - 게시글 저장 시 초성/자모를 미리 계산하여 DB에 저장
   - 검색 성능 대폭 향상

2. **조사 처리 추가**
   - "사과를", "사과가" 등 자동 조사 처리

3. **퍼지 매칭**
   - 오타 허용 검색 (Levenshtein Distance)

4. **검색 히스토리**
   - 사용자별 검색 기록 저장 및 추천

---

##  문의

문제가 있거나 개선 사항이 있으면 이슈를 등록해주세요.

# 학교 정보 페이지 (급식 + 시간표 + 학사일정) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 근명고등학교의 급식(오늘), 시간표(학년/반 선택), 학사일정을 보여주는 STARLOG 공개 페이지 `/school` 추가

**Architecture:** Spring Boot 서버에서 NEIS API(RestTemplate)로 급식·학사일정을, comcigan-parser(Node.js 서브프로세스)로 시간표를 가져와 Thymeleaf로 렌더링. ConcurrentHashMap 기반 인메모리 캐싱 적용.

**Tech Stack:** Java 17, Spring Boot 3.5.7, Thymeleaf, Jackson, Node.js v24 (subprocess), comcigan-parser

---

### Task 1: Node.js 환경 구성 및 comcigan-parser 설치

**Files:**
- Create: `timetable-script/package.json`
- Create: `timetable-fetcher.mjs`

- [ ] **Step 1: Create timetable-script/package.json**

```json
{
  "name": "timetable-fetcher",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "dependencies": {
    "comcigan-parser": "^1.0.0"
  }
}
```

- [ ] **Step 2: npm install**

```bash
npm install --prefix timetable-script
```
Expected: `comcigan-parser` installed in `timetable-script/node_modules/`

- [ ] **Step 3: Create timetable-fetcher.mjs**

```javascript
import Timetable from './timetable-script/node_modules/comcigan-parser/index.js';

const SCHOOL_NAME = '근명고등학교';

async function main() {
    const timetable = new Timetable();
    await timetable.init({ maxGrade: 3 });

    const schools = await timetable.search(SCHOOL_NAME);
    const target = schools.find(s => s.name === SCHOOL_NAME);

    if (!target) {
        console.error('SCHOOL_NOT_FOUND');
        process.exit(1);
    }

    timetable.setSchool(target.code);

    const [classTimes, tableData] = await Promise.all([
        timetable.getClassTime(),
        timetable.getTimetable()
    ]);

    console.log(JSON.stringify({ classTimes, timetable: tableData }));
}

main().catch(err => {
    console.error('FETCH_ERROR:', err.message);
    process.exit(1);
});
```

- [ ] **Step 4: Test the script**

```bash
node timetable-fetcher.mjs
```
Expected: JSON output with `classTimes` and `timetable` data for 근명고등학교

- [ ] **Step 5: Commit**

```bash
git add timetable-script/ timetable-fetcher.mjs
git commit -m "feat: add comcigan-parser timetable fetcher script"
```

---

### Task 2: NEIS API DTO 클래스 작성

**Files:**
- Create: `src/main/java/com/example/demo/dto/neis/NeisMealRow.java`
- Create: `src/main/java/com/example/demo/dto/neis/NeisScheduleRow.java`

- [ ] **Step 1: Create NeisMealRow.java**

```java
package com.example.demo.dto.neis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeisMealRow {
    @JsonProperty("MMEAL_SC_CODE")
    private String mealCode;

    @JsonProperty("MMEAL_SC_NM")
    private String mealName;

    @JsonProperty("DDISH_NM")
    private String dishNames;

    @JsonProperty("ORPLC_INFO")
    private String originInfo;

    @JsonProperty("CAL_INFO")
    private String calInfo;

    @JsonProperty("NTR_INFO")
    private String nutritionInfo;
}
```

- [ ] **Step 2: Create NeisScheduleRow.java**

```java
package com.example.demo.dto.neis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NeisScheduleRow {
    @JsonProperty("AA_YMD")
    private String eventDate;

    @JsonProperty("EVENT_NM")
    private String eventName;

    @JsonProperty("EVENT_CNTNT")
    private String eventContent;
}
```

- [ ] **Step 3: Build to verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/demo/dto/neis/
git commit -m "feat: add NEIS API response DTOs"
```

---

### Task 3: NeisConfig 및 NeisService 구현

**Files:**
- Create: `src/main/java/com/example/demo/config/NeisConfig.java`
- Create: `src/main/java/com/example/demo/service/NeisService.java`

- [ ] **Step 1: Create NeisConfig.java**

```java
package com.example.demo.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class NeisConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
```

- [ ] **Step 2: Create NeisService.java**

```java
package com.example.demo.service;

import com.example.demo.dto.neis.NeisMealRow;
import com.example.demo.dto.neis.NeisScheduleRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@EnableScheduling
public class NeisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${neis.api.key:}")
    private String apiKey;

    @Value("${neis.api.base-url}")
    private String baseUrl;

    @Value("${neis.school.office-code}")
    private String officeCode;

    @Value("${neis.school.code}")
    private String schoolCode;

    private static final String SCRIPT_PATH = "timetable-fetcher.mjs";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ConcurrentHashMap<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    private static class CacheEntry<T> {
        final T data;
        final long expiryTime;

        CacheEntry(T data, long ttlMillis) {
            this.data = data;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private static final long MEAL_TTL = TimeUnit.HOURS.toMillis(1);
    private static final long TIMETABLE_TTL = TimeUnit.HOURS.toMillis(6);
    private static final long SCHEDULE_TTL = TimeUnit.HOURS.toMillis(12);

    public NeisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        log.info("NEIS Service initialized - school: {}/{}", officeCode, schoolCode);
    }

    // ============================================
    // 급식 조회
    // ============================================

    public List<NeisMealRow> getTodayMeal() {
        String today = LocalDate.now().format(DATE_FMT);
        String cacheKey = "meal_" + today;
        return getCached(cacheKey, () -> fetchMeal(today), MEAL_TTL);
    }

    private List<NeisMealRow> fetchMeal(String date) {
        String url = buildNeisUrl("mealServiceDietInfo",
                "ATPT_OFCDC_SC_CODE", officeCode,
                "SD_SCHUL_CODE", schoolCode,
                "MLSV_YMD", date);
        try {
            String json = restTemplate.getForObject(url, String.class);
            return parseNeisResponse(json, "mealServiceDietInfo", NeisMealRow.class);
        } catch (Exception e) {
            log.warn("급식 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ============================================
    // 학사일정 조회
    // ============================================

    public List<NeisScheduleRow> getMonthlySchedule() {
        YearMonth ym = YearMonth.now();
        String from = ym.atDay(1).format(DATE_FMT);
        String to = ym.atEndOfMonth().format(DATE_FMT);
        String cacheKey = "schedule_" + ym;
        return getCached(cacheKey, () -> fetchSchedule(from, to), SCHEDULE_TTL);
    }

    private List<NeisScheduleRow> fetchSchedule(String from, String to) {
        String url = buildNeisUrl("SchoolSchedule",
                "ATPT_OFCDC_SC_CODE", officeCode,
                "SD_SCHUL_CODE", schoolCode,
                "AA_FROM_YMD", from,
                "AA_TO_YMD", to);
        try {
            String json = restTemplate.getForObject(url, String.class);
            return parseNeisResponse(json, "SchoolSchedule", NeisScheduleRow.class);
        } catch (Exception e) {
            log.warn("학사일정 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ============================================
    // 시간표 조회 (comcigan-parser)
    // ============================================

    public JsonNode getTimetableData() {
        return getCached("timetable_full", this::fetchTimetableFromScript, TIMETABLE_TTL);
    }

    public List<Map<String, Object>> getTimetableForClass(int grade, int classNum) {
        JsonNode data = getTimetableData();
        if (data == null) return Collections.emptyList();

        try {
            JsonNode gradeNode = data.path("timetable").path(String.valueOf(grade));
            JsonNode classNode = gradeNode.path(String.valueOf(classNum));

            if (!classNode.isArray()) return Collections.emptyList();

            List<Map<String, Object>> result = new ArrayList<>();
            for (int day = 0; day < classNode.size(); day++) {
                JsonNode dayArray = classNode.get(day);
                if (!dayArray.isArray()) continue;

                for (int period = 0; period < dayArray.size(); period++) {
                    JsonNode item = dayArray.get(period);
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("weekday", item.path("weekdayString").asText());
                    map.put("period", item.path("classTime").asInt());
                    map.put("subject", item.path("subject").asText());
                    map.put("teacher", item.path("teacher").asText());
                    result.add(map);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("시간표 데이터 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> getClassTimes() {
        JsonNode data = getTimetableData();
        if (data == null) return Collections.emptyList();

        try {
            JsonNode classTimes = data.path("classTimes");
            List<String> result = new ArrayList<>();
            if (classTimes.isArray()) {
                for (JsonNode node : classTimes) {
                    result.add(node.asText());
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Map<Integer, Integer> getMaxClassesPerGrade() {
        JsonNode data = getTimetableData();
        Map<Integer, Integer> result = new LinkedHashMap<>();
        if (data == null) return result;

        try {
            JsonNode timetable = data.path("timetable");
            var fields = timetable.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                int grade = Integer.parseInt(entry.getKey());
                int classCount = entry.getValue().size();
                result.put(grade, classCount);
            }
        } catch (Exception e) {
            log.warn("학년/반 정보 파싱 실패: {}", e.getMessage());
        }
        return result;
    }

    // ============================================
    // Node.js 스크립트 실행
    // ============================================

    private JsonNode fetchTimetableFromScript() {
        try {
            ProcessBuilder pb = new ProcessBuilder("node", SCRIPT_PATH);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Node.js 스크립트 실패 (exit {}): {}", exitCode, output);
                return null;
            }

            return objectMapper.readTree(output.toString());
        } catch (Exception e) {
            log.warn("Node.js 프로세스 실행 실패: {}", e.getMessage());
            return null;
        }
    }

    // ============================================
    // 공통 유틸리티
    // ============================================

    private String buildNeisUrl(String endpoint, String... params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("/").append(endpoint);
        sb.append("?KEY=").append(apiKey);
        sb.append("&Type=json");
        for (int i = 0; i < params.length; i += 2) {
            sb.append("&").append(params[i]).append("=").append(params[i + 1]);
        }
        return sb.toString();
    }

    private <T> List<T> parseNeisResponse(String json, String endpoint, Class<T> rowType) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode body = root.path(endpoint);
            if (body.isArray() && body.size() > 1) {
                JsonNode rowNode = body.get(1).get("row");
                if (rowNode != null && rowNode.isArray()) {
                    return objectMapper.readValue(rowNode.traverse(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, rowType));
                }
            }
        } catch (Exception e) {
            log.warn("NEIS 응답 파싱 실패: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getCached(String key, java.util.function.Supplier<List<T>> loader, long ttlMillis) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit: {}", key);
            return (List<T>) entry.data;
        }
        log.debug("Cache miss: {}", key);
        List<T> data = loader.get();
        cache.put(key, new CacheEntry<>(data, ttlMillis));
        return data;
    }

    private JsonNode getCached(String key, java.util.function.Supplier<JsonNode> loader, long ttlMillis) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            log.debug("Cache hit: {}", key);
            return (JsonNode) entry.data;
        }
        log.debug("Cache miss: {}", key);
        JsonNode data = loader.get();
        cache.put(key, new CacheEntry<>(data, ttlMillis));
        return data;
    }

    @Scheduled(cron = "0 0 7 * * ?")
    public void clearMealCache() {
        log.info("아침 7시 - 급식 캐시 초기화");
        cache.keySet().removeIf(k -> k.startsWith("meal_"));
    }
}
```

- [ ] **Step 3: Build to verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/demo/config/NeisConfig.java \
        src/main/java/com/example/demo/service/NeisService.java
git commit -m "feat: add NeisService with NEIS API and comcigan-parser integration"
```

---

### Task 4: application.properties에 NEIS 설정 추가

**Files:**
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/application.properties.template`

- [ ] **Step 1: Add NEIS config to application.properties**

After line 61 (`turnstile.site.key=...`), append:

```properties

# ==========================================
# NEIS API 설정 (학교 급식/일정)
# ==========================================
neis.api.key=${NEIS_API_KEY:3d6c925c01294cd89d272b490d20c952}
neis.api.base-url=https://open.neis.go.kr/hub
neis.school.code=7531381
neis.school.office-code=J10
```

- [ ] **Step 2: Create application.properties.template**

Create `src/main/resources/application.properties.template` with the same content but `neis.api.key=${NEIS_API_KEY:}` (no default key).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.properties \
        src/main/resources/application.properties.template
git commit -m "feat: add NEIS API configuration"
```

---

### Task 5: SchoolController 구현

**Files:**
- Create: `src/main/java/com/example/demo/controller/SchoolController.java`

- [ ] **Step 1: Create SchoolController.java**

```java
package com.example.demo.controller;

import com.example.demo.dto.neis.NeisMealRow;
import com.example.demo.dto.neis.NeisScheduleRow;
import com.example.demo.service.NeisService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SchoolController {

    private final NeisService neisService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    @GetMapping("/school")
    public String school(
            @RequestParam(defaultValue = "1") int grade,
            @RequestParam(defaultValue = "1") int classNum,
            HttpSession session,
            Model model) {

        grade = clamp(grade, 1, 3);
        classNum = clamp(classNum, 1, 15);

        String username = (String) session.getAttribute("loginUser");
        if (username != null) {
            model.addAttribute("username", username);
        }

        model.addAttribute("today", LocalDate.now().format(DATE_FMT));
        model.addAttribute("currentGrade", grade);
        model.addAttribute("currentClass", classNum);

        List<NeisMealRow> meals = neisService.getTodayMeal();
        model.addAttribute("meals", meals);
        model.addAttribute("hasMeals", !meals.isEmpty());

        List<Map<String, Object>> timetable = neisService.getTimetableForClass(grade, classNum);
        model.addAttribute("timetable", timetable);
        model.addAttribute("hasTimetable", !timetable.isEmpty());

        List<String> classTimes = neisService.getClassTimes();
        model.addAttribute("classTimes", classTimes);

        List<NeisScheduleRow> schedules = neisService.getMonthlySchedule();
        model.addAttribute("schedules", schedules);
        model.addAttribute("hasSchedules", !schedules.isEmpty());

        Map<Integer, Integer> maxClasses = neisService.getMaxClassesPerGrade();
        model.addAttribute("maxClass", maxClasses.getOrDefault(grade, 10));

        return "school";
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/example/demo/controller/SchoolController.java
git commit -m "feat: add SchoolController for /school page"
```

---

### Task 6: school.html 템플릿 작성

**Files:**
- Create: `src/main/resources/templates/school.html`

- [ ] **Step 1: Create school.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>학교 - STARLOG</title>
    <link rel="icon" type="image/png" href="/static/images/logo-rounded.png">
    <link rel="stylesheet" href="/static/css/reset.css?v=2">
    <link rel="stylesheet" href="/static/css/school.css?v=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
    <header class="school-header">
        <div class="wrap">
            <div class="header-top">
                <a href="/home" class="header-logo">STARLOG</a>
                <nav class="header-nav">
                    <a href="/posts">학교 커뮤니티</a>
                    <a href="https://kalpha.c01.kr">오늘의 운세</a>
                    <a href="/school" class="active">학교</a>
                </nav>
                <div class="header-menu">
                    <div th:if="${username}">
                        <a href="/mypage" th:text="${username} + '님'"></a>
                        <form th:action="@{/auth/logout}" method="post" style="display:inline;">
                            <button type="submit" class="logout-btn">로그아웃</button>
                        </form>
                    </div>
                    <div th:unless="${username}">
                        <a href="/auth/login">로그인</a>
                        <a href="/auth/register">회원가입</a>
                    </div>
                </div>
            </div>
        </div>
    </header>

    <main class="school-main">
        <div class="wrap">
            <h1 class="page-title">★ 오늘의 학교</h1>
            <p class="page-date" th:text="${today}"></p>

            <div class="school-grid">
                <!-- 왼쪽: 선택기 + 급식 -->
                <div class="school-left">
                    <div class="selector-card">
                        <h2>학년 / 반 선택</h2>
                        <form method="get" action="/school" class="selector-form">
                            <div class="selector-row">
                                <label>학년</label>
                                <select name="grade" onchange="this.form.submit()">
                                    <option th:each="g : ${#numbers.sequence(1, 3)}"
                                            th:value="${g}"
                                            th:text="${g} + '학년'"
                                            th:selected="${g == currentGrade}"></option>
                                </select>
                            </div>
                            <div class="selector-row">
                                <label>반</label>
                                <select name="classNum" onchange="this.form.submit()">
                                    <option th:each="c : ${#numbers.sequence(1, maxClass)}"
                                            th:value="${c}"
                                            th:text="${c} + '반'"
                                            th:selected="${c == currentClass}"></option>
                                </select>
                            </div>
                        </form>
                    </div>

                    <div class="meal-card">
                        <h2>🍽 오늘의 급식</h2>
                        <div th:if="${hasMeals}">
                            <div th:each="meal : ${meals}" class="meal-item">
                                <span class="meal-type" th:text="${meal.mealName}"></span>
                                <div class="meal-dishes">
                                    <span th:each="dish : ${#strings.arraySplit(meal.dishNames, '<br/>')}"
                                          th:text="${dish}"
                                          class="dish"></span>
                                </div>
                            </div>
                        </div>
                        <div th:unless="${hasMeals}" class="no-data">
                            오늘의 급식 정보가 없습니다.
                        </div>
                    </div>
                </div>

                <!-- 오른쪽: 시간표 -->
                <div class="school-right">
                    <div class="timetable-card">
                        <h2>
                            📘 <span th:text="${currentGrade} + '학년 ' + ${currentClass} + '반'"></span> 시간표
                        </h2>
                        <div th:if="${hasTimetable}" class="timetable-wrap">
                            <table class="timetable">
                                <thead>
                                    <tr>
                                        <th>교시</th>
                                        <th>월</th>
                                        <th>화</th>
                                        <th>수</th>
                                        <th>목</th>
                                        <th>금</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr th:each="i : ${#numbers.sequence(1, 7)}">
                                        <td class="period-cell">
                                            <span th:text="${i}"></span>
                                            <small th:if="${classTimes != null && classTimes.size() >= i}"
                                                   th:text="'(' + ${classTimes[i - 1].split('\\(')[1].replace(')', '')} + ')'"></small>
                                        </td>
                                        <td th:each="day : ${#numbers.sequence(1, 5)}">
                                            <div th:with="item=${timetable.stream().filter(t -> t.period == i && t.weekday == (day == 1 ? '월' : day == 2 ? '화' : day == 3 ? '수' : day == 4 ? '목' : '금')).findFirst().orElse(null)}">
                                                <span th:if="${item != null}" class="subject-name" th:text="${item.subject}"></span>
                                                <span th:if="${item != null}" class="teacher-name" th:text="${item.teacher}"></span>
                                                <span th:unless="${item != null}" class="empty-cell">-</span>
                                            </div>
                                        </td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <div th:unless="${hasTimetable}" class="no-data">
                            시간표 정보를 불러올 수 없습니다.
                        </div>
                    </div>
                </div>
            </div>

            <!-- 학사일정 -->
            <div class="schedule-card">
                <h2>📅 이번 달 학사일정</h2>
                <div th:if="${hasSchedules}">
                    <div th:each="s : ${schedules}" class="schedule-item">
                        <span class="schedule-date" th:text="${#strings.substring(s.eventDate, 4, 6)} + '/' + ${#strings.substring(s.eventDate, 6, 8)}"></span>
                        <span class="schedule-name" th:text="${s.eventName}"></span>
                    </div>
                </div>
                <div th:unless="${hasSchedules}" class="no-data">
                    등록된 학사일정이 없습니다.
                </div>
            </div>
        </div>
    </main>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/school.html
git commit -m "feat: add school.html template"
```

---

### Task 7: school.css 스타일 작성

**Files:**
- Create: `src/main/resources/static/css/school.css`

- [ ] **Step 1: Create school.css**

```css
:root {
    --bg-primary: #0a0e1a;
    --bg-card: rgba(20, 25, 50, 0.85);
    --bg-card-hover: rgba(30, 38, 70, 0.9);
    --text-primary: #e0e0f0;
    --text-secondary: #8899bb;
    --accent: #82BEDC;
    --accent-glow: rgba(130, 190, 220, 0.3);
    --border-color: rgba(130, 190, 220, 0.15);
    --meal-bg: rgba(130, 190, 220, 0.05);
}

* { margin: 0; padding: 0; box-sizing: border-box; }

body {
    font-family: 'Cafe24ClassicType', sans-serif;
    background: var(--bg-primary);
    color: var(--text-primary);
    min-height: 100vh;
}

a { color: inherit; text-decoration: none; }

.wrap {
    max-width: 1100px;
    margin: 0 auto;
    padding: 0 24px;
}

/* ==================== Header ==================== */
.school-header {
    padding: 16px 0;
    border-bottom: 1px solid var(--border-color);
    background: rgba(10, 14, 26, 0.95);
    backdrop-filter: blur(10px);
}

.header-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
}

.header-logo {
    font-size: 24px;
    font-weight: 800;
    color: var(--accent);
    text-shadow: 0 0 20px var(--accent-glow);
    letter-spacing: 2px;
}

.header-nav {
    display: flex;
    gap: 28px;
    font-size: 15px;
}

.header-nav a {
    color: var(--text-secondary);
    transition: color 0.3s;
    padding: 4px 0;
    border-bottom: 2px solid transparent;
}

.header-nav a:hover,
.header-nav a.active {
    color: var(--accent);
    border-bottom-color: var(--accent);
}

.header-menu {
    display: flex;
    gap: 16px;
    font-size: 14px;
    color: var(--text-secondary);
    align-items: center;
}

.header-menu a {
    color: var(--accent);
}

.logout-btn {
    background: none;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    font: inherit;
    font-size: 14px;
}

/* ==================== Main ==================== */
.school-main {
    padding: 40px 0 60px;
}

.page-title {
    font-size: 28px;
    color: var(--accent);
    text-align: center;
    text-shadow: 0 0 25px var(--accent-glow);
    margin-bottom: 4px;
}

.page-date {
    font-size: 15px;
    color: var(--text-secondary);
    text-align: center;
    margin-bottom: 36px;
}

/* ==================== Grid ==================== */
.school-grid {
    display: grid;
    grid-template-columns: 280px 1fr;
    gap: 24px;
    margin-bottom: 32px;
}

/* ==================== Cards ==================== */
.selector-card,
.meal-card,
.timetable-card,
.schedule-card {
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 24px;
    backdrop-filter: blur(8px);
}

.selector-card h2,
.meal-card h2,
.timetable-card h2,
.schedule-card h2 {
    font-size: 17px;
    color: var(--accent);
    margin-bottom: 16px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--border-color);
}

/* ==================== Selector ==================== */
.selector-form {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.selector-row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.selector-row label {
    font-size: 14px;
    color: var(--text-secondary);
    width: 36px;
}

.selector-row select {
    flex: 1;
    padding: 8px 12px;
    border-radius: 8px;
    border: 1px solid var(--border-color);
    background: rgba(10, 14, 26, 0.8);
    color: var(--text-primary);
    font-size: 14px;
    font-family: inherit;
    cursor: pointer;
    outline: none;
    transition: border-color 0.3s;
}

.selector-row select:focus {
    border-color: var(--accent);
}

/* ==================== Meal ==================== */
.meal-card {
    margin-top: 24px;
}

.meal-item {
    margin-bottom: 16px;
}

.meal-item:last-child {
    margin-bottom: 0;
}

.meal-type {
    display: inline-block;
    font-size: 13px;
    color: var(--accent);
    background: var(--accent-glow);
    padding: 2px 10px;
    border-radius: 4px;
    margin-bottom: 8px;
}

.meal-dishes {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.dish {
    font-size: 14px;
    color: var(--text-primary);
    padding-left: 4px;
}

/* ==================== Timetable ==================== */
.timetable-wrap {
    overflow-x: auto;
}

.timetable {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
}

.timetable th,
.timetable td {
    padding: 10px 8px;
    text-align: center;
    border: 1px solid var(--border-color);
}

.timetable th {
    background: rgba(130, 190, 220, 0.08);
    color: var(--accent);
    font-weight: 700;
    font-size: 14px;
}

.timetable td {
    vertical-align: top;
}

.period-cell {
    color: var(--text-secondary);
    font-size: 13px;
    min-width: 70px;
}

.period-cell small {
    display: block;
    font-size: 11px;
    color: var(--text-secondary);
    opacity: 0.7;
}

.subject-name {
    display: block;
    color: var(--text-primary);
    font-size: 13px;
    line-height: 1.4;
}

.teacher-name {
    display: block;
    color: var(--text-secondary);
    font-size: 11px;
    margin-top: 2px;
}

.empty-cell {
    color: var(--text-secondary);
    opacity: 0.4;
}

/* ==================== Schedule ==================== */
.schedule-card {
    margin-top: 0;
}

.schedule-item {
    display: flex;
    align-items: baseline;
    gap: 14px;
    padding: 10px 0;
    border-bottom: 1px solid rgba(130, 190, 220, 0.06);
}

.schedule-item:last-child {
    border-bottom: none;
}

.schedule-date {
    font-size: 13px;
    color: var(--accent);
    background: var(--accent-glow);
    padding: 2px 8px;
    border-radius: 4px;
    white-space: nowrap;
    font-weight: 700;
}

.schedule-name {
    font-size: 14px;
    color: var(--text-primary);
}

/* ==================== No Data ==================== */
.no-data {
    text-align: center;
    padding: 24px 0;
    color: var(--text-secondary);
    font-size: 14px;
}

/* ==================== Responsive ==================== */
@media (max-width: 768px) {
    .school-grid {
        grid-template-columns: 1fr;
    }

    .header-top {
        flex-direction: column;
        gap: 12px;
    }

    .header-nav {
        gap: 16px;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/school.css
git commit -m "feat: add school.css styling"
```

---

### Task 8: home.html에 /school 네비 링크 추가

**Files:**
- Modify: `src/main/resources/templates/home.html`

- [ ] **Step 1: Add /school link in home.html header**

Find the `<header>` section in home.html and add `<a href="/school">학교</a>` to the nav:

Change:
```html
<a href="/posts">학교 커뮤니티</a>
<a href="https://kalpha.c01.kr">오늘의 운세</a>
```
To:
```html
<a href="/posts">학교 커뮤니티</a>
<a href="https://kalpha.c01.kr">오늘의 운세</a>
<a href="/school">학교</a>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/home.html
git commit -m "feat: add /school link to home nav"
```

---

### Task 9: 통합 테스트 및 검증

**Files:** none (verification only)

- [ ] **Step 1: Build the project**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Run the application**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev &
sleep 10
```

- [ ] **Step 3: Test /school endpoint**

```bash
curl -s http://localhost:8090/school | head -20
```
Expected: HTML content with "오늘의 학교" title

- [ ] **Step 4: Stop the server**

```bash
kill %1 2>/dev/null
```

- [ ] **Step 5: Commit if any fixes applied**

---

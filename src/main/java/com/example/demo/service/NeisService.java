package com.example.demo.service;

import com.example.demo.dto.neis.NeisMealRow;
import com.example.demo.dto.neis.NeisScheduleRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
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

    // ============================================
    // 급식 조회 (NEIS API)
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
            return parseNeisRows(json, "mealServiceDietInfo", NeisMealRow.class);
        } catch (Exception e) {
            log.warn("급식 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ============================================
    // 학사일정 조회 (NEIS API)
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
            return parseNeisRows(json, "SchoolSchedule", NeisScheduleRow.class);
        } catch (Exception e) {
            log.warn("학사일정 API 호출 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ============================================
    // 시간표 조회 (comcigan-parser via Node.js)
    // ============================================

    public JsonNode getTimetableData() {
        log.info("시간표 데이터 조회 시도");
        JsonNode result = getCachedJson("timetable_full", this::fetchTimetableFromScript, TIMETABLE_TTL);
        log.info("시간표 데이터 조회 결과: {}", result != null ? "성공" : "실패");
        return result;
    }

    public List<Map<String, Object>> getTimetableForClass(int grade, int classNum) {
        JsonNode data = getTimetableData();
        if (data == null) {
            log.info("시간표 데이터가 null입니다");
            return Collections.emptyList();
        }

        try {
            JsonNode gradeNode = data.path("timetable").path(String.valueOf(grade));
            JsonNode classNode = gradeNode.path(String.valueOf(classNum));
            log.info("시간표 {}-{}: gradeNode={}, classNode={}", grade, classNum,
                    gradeNode.isMissingNode() ? "MISSING" : "OK",
                    classNode.isArray() ? "ARRAY(" + classNode.size() + ")" : "NOT_ARRAY");

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

            String json = output.toString();
            int jsonStart = json.indexOf('{');
            if (jsonStart > 0) {
                json = json.substring(jsonStart);
            }
            return objectMapper.readTree(json);
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

    private <T> List<T> parseNeisRows(String json, String endpoint, Class<T> rowType) {
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
            return (List<T>) entry.data;
        }
        List<T> data = loader.get();
        cache.put(key, new CacheEntry<>(data, ttlMillis));
        return data;
    }

    private JsonNode getCachedJson(String key, java.util.function.Supplier<JsonNode> loader, long ttlMillis) {
        CacheEntry<?> entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (JsonNode) entry.data;
        }
        JsonNode data = loader.get();
        if (data != null) {
            cache.put(key, new CacheEntry<>(data, ttlMillis));
        }
        return data;
    }

    @Scheduled(cron = "0 0 7 * * ?")
    public void clearMealCache() {
        cache.keySet().removeIf(k -> k.startsWith("meal_"));
        log.debug("급식 캐시 초기화 완료");
    }
}

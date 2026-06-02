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
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SchoolController {

    private final NeisService neisService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
    private static final List<String> WEEKDAYS = List.of("월", "화", "수", "목", "금");

    @GetMapping("/school")
    public String school(
            @RequestParam(defaultValue = "1") int grade,
            @RequestParam(defaultValue = "1") int classNum,
            HttpSession session,
            Model model) {

        grade = clamp(grade, 1, 3);

        Map<Integer, Integer> maxClasses = neisService.getMaxClassesPerGrade();
        int maxClass = maxClasses.getOrDefault(grade, 8);
        classNum = clamp(classNum, 1, maxClass);

        String username = (String) session.getAttribute("loginUser");
        if (username != null) {
            model.addAttribute("username", username);
        }

        model.addAttribute("today", LocalDate.now().format(DATE_FMT));
        model.addAttribute("currentGrade", grade);
        model.addAttribute("currentClass", classNum);
        model.addAttribute("maxClass", maxClass);

        List<NeisMealRow> meals = neisService.getTodayMeal();
        model.addAttribute("meals", meals);

        List<Map<String, Object>> timetableData = neisService.getTimetableForClass(grade, classNum);

        List<TimetableCell> timetableRows = new ArrayList<>();
        for (int period = 1; period <= 8; period++) {
            Map<String, String> cells = new LinkedHashMap<>();
            for (String day : WEEKDAYS) {
                cells.put(day, "");
            }
            for (Map<String, Object> row : timetableData) {
                if ((int) row.get("period") == period) {
                    String day = (String) row.get("weekday");
                    String subj = (String) row.get("subject");
                    String tchr = (String) row.get("teacher");
                    if (subj != null && !subj.isEmpty()) {
                        cells.put(day, subj + (tchr != null && !tchr.isEmpty() ? "|" + tchr : ""));
                    }
                }
            }
            timetableRows.add(new TimetableCell(period, cells));
        }

        model.addAttribute("weekdays", WEEKDAYS);
        model.addAttribute("timetableRows", timetableRows);
        model.addAttribute("cellMap", new HashMap<String, String>());

        List<String> classTimes = neisService.getClassTimes();
        List<String> periodTimes = new ArrayList<>();
        for (int i = 0; i < 8 && i < classTimes.size(); i++) {
            String time = classTimes.get(i);
            int parenIdx = time.indexOf('(');
            periodTimes.add(parenIdx >= 0 ? time.substring(parenIdx) : time);
        }
        model.addAttribute("periodTimes", periodTimes);

        List<NeisScheduleRow> schedules = neisService.getMonthlySchedule();
        model.addAttribute("schedules", schedules);

        return "school";
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static class TimetableCell {
        private final int period;
        private final Map<String, String> days;

        public TimetableCell(int period, Map<String, String> days) {
            this.period = period;
            this.days = days;
        }

        public int getPeriod() { return period; }
        public Map<String, String> getDays() { return days; }
        public String getSubject(String day) {
            String val = days.getOrDefault(day, "");
            int pipeIdx = val.indexOf('|');
            return pipeIdx >= 0 ? val.substring(0, pipeIdx) : val;
        }
        public String getTeacher(String day) {
            String val = days.getOrDefault(day, "");
            int pipeIdx = val.indexOf('|');
            return pipeIdx >= 0 ? val.substring(pipeIdx + 1) : "";
        }
    }
}

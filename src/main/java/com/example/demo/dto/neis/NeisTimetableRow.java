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
public class NeisTimetableRow {
    @JsonProperty("ALL_TI_YMD")
    private String date;

    @JsonProperty("PERIO")
    private String period;

    @JsonProperty("ITRT_CNTNT")
    private String subject;

    @JsonProperty("GRADE")
    private String grade;

    @JsonProperty("CLASS_NM")
    private String className;
}

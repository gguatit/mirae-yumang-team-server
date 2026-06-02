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

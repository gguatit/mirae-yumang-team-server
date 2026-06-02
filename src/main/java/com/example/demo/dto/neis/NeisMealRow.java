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

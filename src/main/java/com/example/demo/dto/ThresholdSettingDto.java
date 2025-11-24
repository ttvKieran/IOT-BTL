package com.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ThresholdSettingDto {
    @JsonProperty("deviceUid")
    private String deviceUid;
    @JsonProperty("minSoilMoisture")
    private BigDecimal minSoilMoisture;
    @JsonProperty("maxPumpDurationSeconds")
    private Integer maxPumpDurationSeconds;
    @JsonProperty("isActive")
    private Boolean isActive;
}

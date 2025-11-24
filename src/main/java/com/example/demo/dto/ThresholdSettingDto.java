package com.example.demo.dto;

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
    private String deviceUid;
    private BigDecimal minSoilMoisture;
    private Integer maxPumpDurationSeconds;
    private Boolean isActive;
}

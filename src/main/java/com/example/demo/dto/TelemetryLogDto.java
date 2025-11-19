package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu cảm biến (Telemetry Log DTO)")
public class TelemetryLogDto {

    @Schema(description = "ID log", example = "1001")
    private Long id;

    @NotNull(message = "Device ID không được để trống")
    @Schema(description = "ID của thiết bị", example = "1", required = true)
    private DeviceDto device;

    @NotNull(message = "Thời điểm log không được để trống")
    @Schema(description = "Thời điểm ghi log", example = "2025-10-22T07:45:00")
    private Instant logTime;

    @Schema(description = "Nhiệt độ (°C)", example = "28.5")
    private BigDecimal temperature;

    @Schema(description = "Độ ẩm không khí (%)", example = "60.0")
    private BigDecimal airHumidity;

    @Schema(description = "Cường độ ánh sáng (lux)", example = "1200.50")
    private BigDecimal lightLevel;

    @Schema(description = "Độ ẩm đất (%)", example = "45.75")
    private BigDecimal soilMoisture;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}

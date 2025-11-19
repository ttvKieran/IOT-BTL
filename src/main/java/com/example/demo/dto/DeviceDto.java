package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin thiết bị (Device DTO)")
public class DeviceDto {

    @Schema(description = "ID của thiết bị", example = "1")
    private Long id;


    @NotBlank(message = "Device UID không được để trống")
    @Size(max = 100, message = "Device UID tối đa 100 ký tự")
    @Schema(description = "UID thiết bị (do ESP32 cung cấp)", example = "ESP32_GARDEN_01", required = true)
    @JsonProperty(value = "device_uid")
    private String deviceUid;

    @NotBlank(message = "Tên thiết bị không được để trống")
    @Size(max = 255, message = "Tên thiết bị tối đa 255 ký tự")
    @Schema(description = "Tên thiết bị", example = "Máy bơm nước vườn sau", required = true)
    @JsonProperty(value = "name")
    private String name;

    @Schema(description = "Thời điểm tạo", example = "2025-10-22T08:15:30")
    private Instant createdAt;

    @Schema(description = "Thời điểm cập nhật", example = "2025-10-22T09:10:00")
    private Instant updatedAt;

    @Schema(description = "Thời điểm xóa mềm (nếu có)", example = "2025-10-25T00:00:00")
    private Instant deletedAt;
}

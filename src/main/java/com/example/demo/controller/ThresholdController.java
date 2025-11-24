package com.example.demo.controller;

import com.example.demo.dto.ThresholdSettingDto;
import com.example.demo.model.response.ApiResponse;
import com.example.demo.service.ThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/thresholds")
@RequiredArgsConstructor
@Tag(name = "Threshold Controller", description = "Quản lý cài đặt ngưỡng tự động")
public class ThresholdController {
    private final ThresholdService thresholdService;

    @Operation(summary = "Lấy cài đặt ngưỡng của thiết bị")
    @GetMapping("/{deviceUid}")
    public ResponseEntity<ApiResponse<ThresholdSettingDto>> getSettings(@PathVariable String deviceUid) {
        ThresholdSettingDto settings = thresholdService.getSetting(deviceUid);
        return ResponseEntity.ok(ApiResponse.<ThresholdSettingDto>builder()
                .code(200).message("Success").data(settings).build());
    }

    @Operation(summary = "Lưu cài đặt ngưỡng")
    @PostMapping
    public ResponseEntity<ApiResponse<ThresholdSettingDto>> saveSettings(@RequestBody ThresholdSettingDto dto) {
        dto.setDeviceUid("ESP32_GARDEN_001");
        ThresholdSettingDto saved = thresholdService.saveSetting(dto);
        return ResponseEntity.ok(ApiResponse.<ThresholdSettingDto>builder()
                .code(200).message("Settings saved").data(saved).build());
    }
}

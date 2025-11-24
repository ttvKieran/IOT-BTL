package com.example.demo.controller;

import com.example.demo.dto.CommandRequestDTO;
import com.example.demo.dto.DeviceDto;
import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.dto.TelemetryLogDto;
import com.example.demo.model.response.ApiResponse;
import com.example.demo.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Controller")
public class DeviceController {
    private final DeviceService deviceService;
    private final DeviceStateService deviceStateService; // Cache Redis
    private final TelemetryService telemetryService;     // Lịch sử MySQL
    private final CommandService commandService;


    @Operation(summary = "Lấy danh sách các thiết bị")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeviceDto>>> getMyDevices() {

        List<DeviceDto> devices = deviceService.getAllDevices();

        return ResponseEntity.ok(ApiResponse.<List<DeviceDto>>builder()
                .code(200)
                .message("Successfully retrieved user's devices.")
                .data(devices)
                .build());
    }

    // 3. ENDPOINT: Lấy trạng thái TỨC THỜI của thiết bị (từ Cache)
    @Operation(summary = "Lấy trạng thái TỨC THỜI (real-time) của thiết bị (từ Redis Cache)")
    @GetMapping("/{deviceUid}/state")
    public ResponseEntity<ApiResponse<DeviceStateDTO>> getDeviceState(
            @PathVariable String deviceUid) throws AccessDeniedException {

        DeviceStateDTO state = deviceStateService.getState(deviceUid);

        return ResponseEntity.ok(ApiResponse.<DeviceStateDTO>builder()
                .code(200)
                .message("Successfully retrieved device state.")
                .data(state)
                .build());
    }

    // 4. ENDPOINT: Lấy lịch sử dữ liệu (từ CSDL)
    @Operation(summary = "Lấy lịch sử dữ liệu cảm biến trong khoảng thời gian (từ MySQL)")
    @GetMapping("/{deviceUid}/history")
    @Transactional
    public ResponseEntity<ApiResponse<List<TelemetryLogDto>>> getDeviceHistory(
            @PathVariable String deviceUid,
            @RequestParam Long from,
            @RequestParam Long to) throws AccessDeniedException {

        // Chuyển epoch milliseconds thành Instant
        Instant fromInstant = Instant.ofEpochMilli(from);
        Instant toInstant = Instant.ofEpochMilli(to);

        // Cần giả định phương thức này tồn tại trong TelemetryService
        List<TelemetryLogDto> history = telemetryService.getHistory(deviceUid, fromInstant, toInstant);

        return ResponseEntity.ok(ApiResponse.<List<TelemetryLogDto>>builder()
                .code(200)
                .message("Successfully retrieved device history.")
                .data(history)
                .build());
    }

    // 5. ENDPOINT: Gửi một lệnh điều khiển xuống thiết bị
    @Operation(summary = "Gửi một lệnh điều khiển xuống thiết bị qua MQTT Outbound")
    @PostMapping("/{deviceUid}/command")
    public ResponseEntity<ApiResponse<Void>> sendCommand(
            @PathVariable String deviceUid,
            @Valid @RequestBody CommandRequestDTO command) throws AccessDeniedException {


        commandService.sendCommand(deviceUid, command);

        // Trả về HTTP 202 Accepted (Đã chấp nhận yêu cầu)
        return ResponseEntity.accepted().body(ApiResponse.<Void>builder()
                .code(202)
                .message("Command sent successfully to device.")
                .build());
    }

    //DeviceDto createDevice(DeviceDto deviceDto)
    @Operation(summary = "Tạo thiết bị mới")
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceDto>> createDevice(
            @Valid @RequestBody DeviceDto deviceDto) {
        DeviceDto createdDevice = deviceService.createDevice(deviceDto);
        return ResponseEntity.ok(ApiResponse.<DeviceDto>builder()
                .code(200)
                .message("Device created successfully.")
                .data(createdDevice)
                .build());
    }

    //DeviceDto updateDevice(String deviceUid, DeviceDto deviceDto)
    @Operation(summary = "Cập nhật thông tin thiết bị")
    @PutMapping("/{deviceUid}")
    public ResponseEntity<ApiResponse<DeviceDto>> updateDevice(
            @PathVariable String deviceUid,
            @Valid @RequestBody DeviceDto deviceDto) {
        DeviceDto updatedDevice = deviceService.updateDevice(deviceUid, deviceDto);
        return ResponseEntity.ok(ApiResponse.<DeviceDto>builder()
                .code(200)
                .message("Device updated successfully.")
                .data(updatedDevice)
                .build());
    }

    //void softDeleteDevice(String deviceUid)
    @Operation(summary = "Xóa mềm thiết bị")
    @DeleteMapping("/{deviceUid}")
    public ResponseEntity<ApiResponse<Void>> softDeleteDevice(
            @PathVariable String deviceUid) {
        deviceService.softDeleteDevice(deviceUid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Device soft deleted successfully.")
                .build());
    }

    // void restoreDevice(String deviceUid)
    @Operation(summary = "Khôi phục thiết bị đã xóa mềm")
    @PostMapping("/{deviceUid}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreDevice(
            @PathVariable String deviceUid) {
        deviceService.restoreDevice(deviceUid);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Device restored successfully.")
                .build());
    }

    @PostMapping(value = "/{deviceUid}/auto-off")
    public ResponseEntity<ApiResponse<Void>> turnOffAutoMode(
            @PathVariable String deviceUid,
            @RequestParam boolean autoOff) {
        deviceService.setAutoMode(deviceUid, autoOff);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("Auto mode updated successfully.")
                .build());
    }
}

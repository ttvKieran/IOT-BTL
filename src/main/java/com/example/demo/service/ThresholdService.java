package com.example.demo.service;
import com.example.demo.dto.CommandRequestDTO;
import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.dto.ThresholdSettingDto;
import com.example.demo.model.entity.ThresholdSetting;
import com.example.demo.repository.ThresholdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdService {
    private final ThresholdRepository thresholdRepository;
    private final CommandService commandService;
    private final DeviceStateService deviceStateService;
    private final TaskScheduler taskScheduler; // Dùng để hẹn giờ tắt

    // Map để lưu trạng thái "đang bơm tự động" để tránh gửi lệnh lặp lại
    private final Map<String, Boolean> isPumpingMap = new ConcurrentHashMap<>();

    // 1. Lưu/Cập nhật cài đặt
    @Transactional
    public ThresholdSettingDto saveSetting(ThresholdSettingDto dto) {
        ThresholdSetting setting = thresholdRepository.findByDeviceUid(dto.getDeviceUid())
                .orElse(ThresholdSetting.builder().deviceUid(dto.getDeviceUid()).build());

        setting.setMinSoilMoisture(dto.getMinSoilMoisture());
        setting.setMaxPumpDurationSeconds(dto.getMaxPumpDurationSeconds());
        setting.setIsActive(dto.getIsActive());

        ThresholdSetting saved = thresholdRepository.save(setting);
        return mapToDto(saved);
    }

    // 2. Lấy cài đặt
    public ThresholdSettingDto getSetting(String deviceUid) {
        return thresholdRepository.findByDeviceUid(deviceUid)
                .map(this::mapToDto)
                .orElse(new ThresholdSettingDto(deviceUid, BigDecimal.valueOf(0), 10, false));
    }

    // 3. Logic kiểm tra và kích hoạt bơm (Được gọi mỗi khi có Telemetry)
    public void checkAndAutomate(String deviceUid, DeviceStateDTO.SensorData sensorData) {
        // Lấy cài đặt từ DB
        Optional<ThresholdSetting> settingOpt = thresholdRepository.findByDeviceUid(deviceUid);
        if (settingOpt.isEmpty() || !settingOpt.get().getIsActive()) {
            return; // Không có cài đặt hoặc tính năng đang tắt
        }

        ThresholdSetting setting = settingOpt.get();
        double currentMoisture = sensorData.getSoilMoisture();
        double threshold = setting.getMinSoilMoisture().doubleValue();

        // Logic: Nếu đất khô hơn ngưỡng VÀ chưa đang bơm
        if (currentMoisture < threshold && !isPumping(deviceUid)) {
            log.info("AUTO-RULE: Đất khô ({}%) < Ngưỡng ({}%). Kích hoạt bơm trong {}s.",
                    currentMoisture, threshold, setting.getMaxPumpDurationSeconds());

            // B1: Gửi lệnh BẬT Bơm
            sendCommand(deviceUid, "ON");
            setPumping(deviceUid, true);

            // B2: Hẹn giờ TẮT Bơm
            taskScheduler.schedule(() -> {
                log.info("AUTO-RULE: Hết thời gian bơm ({}s). Gửi lệnh TẮT.", setting.getMaxPumpDurationSeconds());
                sendCommand(deviceUid, "OFF");
                setPumping(deviceUid, false);
            }, Instant.now().plusSeconds(setting.getMaxPumpDurationSeconds()));
        }
    }

    private void sendCommand(String deviceUid, String state) {
        CommandRequestDTO command = new CommandRequestDTO();
        command.setAction("CONTROL_PUMP");
        command.setPayload(Map.of("state", state));
        commandService.sendCommand(deviceUid, command);
    }

    private boolean isPumping(String deviceUid) {
        // Kiểm tra thêm trạng thái thực tế từ Redis để chắc chắn
        DeviceStateDTO state = deviceStateService.getState(deviceUid);
        boolean redisState = "ON".equalsIgnoreCase(state.getPumpState());
        return isPumpingMap.getOrDefault(deviceUid, false) || redisState;
    }

    private void setPumping(String deviceUid, boolean status) {
        isPumpingMap.put(deviceUid, status);
    }

    private ThresholdSettingDto mapToDto(ThresholdSetting entity) {
        return new ThresholdSettingDto(
                entity.getDeviceUid(),
                entity.getMinSoilMoisture(),
                entity.getMaxPumpDurationSeconds(),
                entity.getIsActive()
        );
    }
}

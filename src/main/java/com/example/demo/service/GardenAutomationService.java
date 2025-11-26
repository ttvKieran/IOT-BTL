package com.example.demo.service;

import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GardenAutomationService {
    private final AiChatService aiChatService;
    private final NotificationService notificationService;
    private final DeviceRepository deviceRepository;

    private static final String DEFAULT_DEVICE_UID = "ESP32_GARDEN_001";
    private static final String DEFAULT_MESSAGE = "Vui lòng phân tích trạng thái hiện tại của vườn và đề xuất các hành động tự động để duy trì sức khỏe cây trồng. Nếu số liệu không quá khác biệt hay cần thiết thì không cần đề xuất hành động gì.";

    @Scheduled(cron = "0 */1 * * * *")
    public void runProactiveAutomation() {
        // Check directly from database - no Redis cache issues
        var device = deviceRepository.findByDeviceUid(DEFAULT_DEVICE_UID);
        
        if(device.isEmpty()) {
            log.warn("Device {} not found in database", DEFAULT_DEVICE_UID);
            return;
        }
        
        boolean isAutoMode = device.get().isAutoMode();
        log.info("GardenAutomationService - Device: {}, isAutoMode: {}", DEFAULT_DEVICE_UID, isAutoMode);
        
        if(isAutoMode) {
            // AUTO mode -> AI phân tích và execute tool call (bật máy bơm tự động)
            log.info("GardenAutomationService is in AUTO mode, calling AI for proactive automation.");
            String aiDecision = aiChatService.analysis(DEFAULT_MESSAGE , DEFAULT_DEVICE_UID);
            log.info("GardenAutomationService AI Decision (AUTO): " + aiDecision);
        }
        else{
            // MANUAL mode -> AI phân tích nhưng CHỈ GỬI EMAIL, không execute tool call
            log.info("GardenAutomationService is in MANUAL mode, AI will only send email notifications.");
            String aiDecision = aiChatService.getChatResponse(DEFAULT_MESSAGE , DEFAULT_DEVICE_UID);
            log.info("GardenAutomationService AI Decision (MANUAL - email only): " + aiDecision);
        }
    }
}

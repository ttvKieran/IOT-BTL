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
    private static final String REDIS_KEY_PREFIX = "device:autoMode:";
    private final RedisTemplate<String, DeviceStateDTO> deviceStateRedisTemplate;
    private static String autoModeValue;

    private String createKey(String deviceUid) {
        return REDIS_KEY_PREFIX + deviceUid;
    }


    private static final String DEFAULT_DEVICE_UID = "ESP32_GARDEN_001";
    private static final String DEFAULT_MESSAGE = "Vui lòng phân tích trạng thái hiện tại của vườn và đề xuất các hành động tự động để duy trì sức khỏe cây trồng. Nếu số liệu không quá khác biệt hay cần thiết thì không cần đề xuất hành động gì.";

    @Scheduled(cron = "0 */1 * * * *")
    public void runProactiveAutomation() {
        String key = createKey(DEFAULT_DEVICE_UID);
        DeviceStateDTO deviceStateDTO = deviceStateRedisTemplate.opsForValue().get(key);
        if(deviceStateDTO == null){
            autoModeValue = "MANUAL";
        }
        else {
            autoModeValue = deviceStateDTO.getControlMode();
        }
        if(autoModeValue == null || !autoModeValue.equals("AUTO")) {
            String aiDecision = aiChatService.analysis(DEFAULT_MESSAGE , DEFAULT_DEVICE_UID);
            log.info("GardenAutomationService AI Decision MANUAL : " + aiDecision);
        }
        else{
            log.info("GardenAutomationService is in AUTO mode, skipping proactive automation.");
            String aiDecision = aiChatService.getChatResponse(DEFAULT_MESSAGE , DEFAULT_DEVICE_UID);
            log.info("GardenAutomationService AI Decision: " + aiDecision);
        }
    }
}

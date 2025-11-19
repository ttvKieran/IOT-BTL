package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GardenAutomationService {
    private final AiChatService aiChatService;
    private final NotificationService notificationService;

    private static String lastAIMessage = "";


    private static final String DEFAULT_DEVICE_UID = "ESP32_GARDEN_01";
    private static final String DEFAULT_MESSAGE = "Vui lòng phân tích trạng thái hiện tại của vườn và đề xuất các hành động tự động để duy trì sức khỏe cây trồng.";

    @Scheduled(cron = "0 */2 * * * *")
    public void runProactiveAutomation() {
        log.info("GardenAutomationService runProactiveAutomation");
        try{
            String aiDecision = aiChatService.analysis(DEFAULT_MESSAGE , DEFAULT_DEVICE_UID);
            if(aiDecision.equals(lastAIMessage)){
                log.info("GardenAutomationService AI Decision unchanged.");
                return;
            }
            lastAIMessage = aiDecision;
            notificationService.broadcastAIMessage(aiDecision);
            log.info("GardenAutomationService AI Decision: " + aiDecision);
        }
        catch (Exception e){
            log.error("GardenAutomationService runProactiveAutomation error: " + e.getMessage());
        }
        log.info("GardenAutomationService runProactiveAutomation");
    }
}

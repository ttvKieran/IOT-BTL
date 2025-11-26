package com.example.demo.controller;

import com.example.demo.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-notifications")
@RequiredArgsConstructor
@Tag(name = "Test Notification Controller", description = "Controller for sending test notifications to devices")
public class TestNotificationController {
    private final NotificationService notificationService;
    @PostMapping
    public ResponseEntity<?> sendTestNotification(@RequestParam String messange) {
        // Logic to send a test notification to the device with the given UID
        // This is a placeholder for the actual implementation
        System.out.println("===== TEST CONTROLLER CALLED with message: " + messange);
        notificationService.broadcastAIMessage(messange);
        System.out.println("===== broadcastAIMessage() completed");
        return ResponseEntity.ok("Test notification sent to all devices.");

    }
}

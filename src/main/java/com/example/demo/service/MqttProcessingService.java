package com.example.demo.service;

import com.example.demo.dto.DeviceStateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MqttProcessingService {

    private final ObjectMapper objectMapper;
    private final DeviceStateService deviceStateService; // Quản lý Redis Cache
    private final TelemetryService telemetryService;     // Lưu vào MySQL (Async)
    private final NotificationService notificationService; // Đẩy qua WebSocket

    /**
     * ServiceActivator lắng nghe kênh "mqttInputChannel" đã cấu hình trong MqttConfig.
     * Đây là nơi tất cả tin nhắn thiết bị đến được xử lý.
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleIncomingMqtt(Message<String> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String payload = message.getPayload();
        log.debug("INBOUND MQTT: Topic=[{}], Payload=[{}]", topic, payload);

        try {
            // 1. Phân tích Topic: smartgarden/device/{deviceUid}/{messageType}
            String[] parts = topic.split("/");
            if (parts.length < 4) {
                log.error("Invalid MQTT topic format: {}", topic);
                return;
            }

            String deviceUid = parts[2];
            String messageType = parts[3];

            // 2. BƯỚC 1: Cập nhật Cache (Redis)
            // deviceStateService sẽ lấy DTO cũ từ Redis, cập nhật và lưu lại.
            DeviceStateDTO updatedState = deviceStateService.updateStateFromMqtt(deviceUid, messageType, payload);

            if ("telemetry".equals(messageType.toLowerCase())) {
                // Phương thức này là @Async, không làm block thread MQTT hiện tại
                telemetryService.saveTelemetryLog(updatedState);
            }
            notificationService.broadcastDeviceUpdate(updatedState);

        } catch (Exception e) {
            // Ghi log lỗi mà không làm ảnh hưởng đến việc xử lý các tin nhắn MQTT khác
            log.error("Failed to process MQTT message from topic {}: {}", topic, e.getMessage(), e);
        }
    }
}
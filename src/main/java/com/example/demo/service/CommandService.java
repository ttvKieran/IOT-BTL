package com.example.demo.service;

import com.example.demo.configuration.MqttConfig;
import com.example.demo.dto.CommandRequestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandService {

    private final MqttConfig.MqttOutboundGateway mqttOutboundGateway;
    private final ObjectMapper objectMapper; // Dùng để chuyển đổi DTO sang JSON String
    private final DeviceStateService deviceStateService; // Cập nhật Redis ngay lập tức
    private final NotificationService notificationService; // Broadcast qua WebSocket

    /**
     * Gửi một lệnh điều khiển xuống thiết bị IoT qua MQTT.
     *
     * @param deviceUid ID duy nhất của thiết bị đích.
     * @param command CommandRequestDTO chứa hành động và payload.
     */
    synchronized public void sendCommand(String deviceUid, CommandRequestDTO command) {

        // 1. Tạo payload từ CommandRequestDTO
        // Tạo một cấu trúc dữ liệu đơn giản hơn để gửi qua MQTT nếu cần,
        // nhưng ở đây ta gửi toàn bộ DTO sau khi đã chuẩn hóa.
        String jsonPayload;
        try {
            // ObjectMapper chuyển đổi CommandRequestDTO (action, payload) thành chuỗi JSON
            jsonPayload = objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert command DTO to JSON for device {}: {}", deviceUid, e.getMessage());
            throw new RuntimeException("Error serializing command payload.", e);
        }

        // 2. Tính toán topic gửi
        // Cấu trúc topic: smartgarden/command/{deviceUid}
        // (Hoặc smartgarden/device/{deviceUid}/command, tùy theo giao thức thống nhất với ESP32)
        final String commandTopic = String.format("smartgarden/device/%s/command", deviceUid);

        log.info("Sending command to topic: {} with payload: {}", commandTopic, jsonPayload);

        // 3. Cập nhật trạng thái trong Redis NGAY LẬP TỨC trước khi gửi lệnh
        // Điều này đảm bảo UI đồng bộ ngay cả khi ESP32 phản hồi chậm
        updateStateImmediately(deviceUid, command);

        // 4. Gọi interface MqttOutboundGateway
        // Gateway sẽ tự động gửi tin nhắn này vào kênh MQTT đã cấu hình.
        mqttOutboundGateway.sendToMqtt(jsonPayload, commandTopic);
    }

    /**
     * Cập nhật trạng thái trong Redis ngay lập tức khi gửi lệnh
     */
    private void updateStateImmediately(String deviceUid, CommandRequestDTO command) {
        try {
            if ("CONTROL_PUMP".equals(command.getAction())) {
                // Lấy trạng thái hiện tại từ Redis
                var currentState = deviceStateService.getState(deviceUid);
                
                // Cập nhật pump state từ payload
                if (command.getPayload() != null && command.getPayload().containsKey("state")) {
                    String pumpState = (String) command.getPayload().get("state");
                    currentState.setPumpState(pumpState);
                    log.info("Immediately updating pump state to: {} for device: {}", pumpState, deviceUid);
                    
                    // Lưu lại vào Redis
                    deviceStateService.updateStateFromMqtt(deviceUid, "state", 
                        objectMapper.writeValueAsString(currentState));
                    
                    // Broadcast qua WebSocket để frontend cập nhật ngay
                    notificationService.broadcastDeviceUpdate(currentState);
                }
            } else if ("SET_MODE".equals(command.getAction())) {
                // Tương tự cho control mode
                var currentState = deviceStateService.getState(deviceUid);
                if (command.getPayload() != null && command.getPayload().containsKey("mode")) {
                    String mode = (String) command.getPayload().get("mode");
                    currentState.setControlMode(mode);
                    log.info("Immediately updating control mode to: {} for device: {}", mode, deviceUid);
                    
                    deviceStateService.updateStateFromMqtt(deviceUid, "state",
                        objectMapper.writeValueAsString(currentState));
                    notificationService.broadcastDeviceUpdate(currentState);
                }
            }
        } catch (Exception e) {
            log.error("Failed to immediately update state for device {}: {}", deviceUid, e.getMessage());
        }
    }
}
package com.example.demo.service;

import com.example.demo.dto.DeviceStateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // SimpMessagingTemplate là thành phần cốt lõi để gửi tin nhắn qua WebSocket
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi cập nhật trạng thái tức thời của thiết bị tới tất cả các client đã subscribe.
     *
     * @param state DeviceStateDTO đã được cập nhật từ luồng MQTT/Redis.
     */
    public void broadcastDeviceUpdate(DeviceStateDTO state) {
        // Địa chỉ topic mà client cần subscribe: /topic/device/{deviceUid}
        String destination = "/topic/device/" + state.getDeviceUid();

        try {
            // Gửi toàn bộ DeviceStateDTO (dưới dạng JSON) tới topic.
            // Broker (được cấu hình bằng enableSimpleBroker("/topic")) sẽ phân phối tin nhắn này.
            messagingTemplate.convertAndSend(destination, state);
            log.debug("Broadcasted device state update to topic: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket message to {}: {}", destination, e.getMessage());
            // Xử lý lỗi (ví dụ: client disconnect)
        }
    }

    public void broadcastAIMessage(String aiMessage) {
        String destination = "/topic/ai/chat";
        try {
            // Gửi tin nhắn AI tới topic thong bao cho Admin
            messagingTemplate.convertAndSend(destination, aiMessage);
            log.debug("Broadcasted AI message to topic: {}", destination);
        } catch (Exception e) {
            log.error("Failed to broadcast AI WebSocket message to {}: {}", destination, e.getMessage());
        }
    }
}
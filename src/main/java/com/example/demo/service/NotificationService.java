package com.example.demo.service;

import com.example.demo.dto.DeviceStateDTO;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    // SimpMessagingTemplate là thành phần cốt lõi để gửi tin nhắn qua WebSocket
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;


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

//    @Async
    public void broadcastAIMessage(String aiMessage) {
        MimeMessage message = mailSender.createMimeMessage();
        log.debug("Broadcasting AI Message to topic: {}", aiMessage);
        try {

            MimeMailMessage mimeMailMessage = new MimeMailMessage(message);
            mimeMailMessage.setFrom(fromEmail);
            mimeMailMessage.setTo("tatruongvu1708@gmail.com");
            mimeMailMessage.setSubject("AI Notification");
            mimeMailMessage.setText("" +
                    "" +
                    "Xin chào,\n\n" +
                    "Đây là thông báo từ hệ thống AI của vườn thông minh:\n"
            + aiMessage +
                    "\n\nTrân trọng,\nHệ thống Vườn Thông Minh");
            mailSender.send(mimeMailMessage.getMimeMessage());
            log.info("AI notification email sent successfully.");
        } catch (Exception e) {
            log.error("Failed to send AI notification email: {}", e.getMessage());
        }
    }
}
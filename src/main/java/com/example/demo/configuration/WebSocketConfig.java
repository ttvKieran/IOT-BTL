package com.example.demo.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Kích hoạt xử lý tin nhắn WebSocket bằng broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 1. Cấu hình Message Broker (Nơi định tuyến tin nhắn)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple Broker: Dùng để đẩy tin nhắn từ server đến client
        // Tất cả các topic bắt đầu bằng "/topic" sẽ được broker xử lý và gửi đến các client đã subscribe.
        config.enableSimpleBroker("/topic");

        // Application Destination Prefixes: Dùng để nhận tin nhắn từ client (Dashboard)
        // Các tin nhắn gửi đến server (ví dụ: yêu cầu gửi lệnh điều khiển qua WebSocket)
        // phải bắt đầu bằng "/app" (ví dụ: /app/send-command).
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 2. Đăng ký STOMP Endpoint
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint kết nối WebSocket cho client.
        // Client sẽ kết nối tới ws://<hostname>:<port>/ws
        // .withSockJS() được thêm vào để hỗ trợ kết nối qua các trình duyệt không hỗ trợ WebSocket native.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Cho phép tất cả các nguồn (cần cấu hình lại cho sản phẩm thực tế)
                .withSockJS();
    }
}
package com.example.demo.controller;

import com.example.demo.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Cho phép CORS từ mọi nguồn (dành cho phát triển Frontend)
public class AiChatController {

    private final AiChatService aiChatService; // Tiêm (inject) service AI

    /**
     * Endpoint chính để chat với AI.
     * Người dùng gửi tin nhắn (dưới dạng text) và deviceUid (qua URL).
     */
    @PostMapping("/chat/{deviceUid}")
    @Operation(summary = "Gửi tin nhắn chat đến AI của vườn")
    public ResponseEntity<ChatResponseDTO> handleChat(
            @PathVariable String deviceUid,
            @Valid @RequestBody ChatRequestDTO request) {

        log.info("AI Chat request for device {}: {}", deviceUid, request.getMessage());

        // 1. Lấy tin nhắn từ request
        String userMessage = request.getMessage();

        // 2. Gọi service AI chính (AiChatService)
        // Service này sẽ lo việc: lấy cache, gọi API thời tiết, và gọi Python
        String aiResponse = aiChatService.getChatResponse(
                userMessage,
                deviceUid
        );

        // 3. Trả về phản hồi của AI cho Frontend
        return ResponseEntity.ok(new ChatResponseDTO(aiResponse));
    }

    // --- DTOs cho API ---
    // (DTO: Data Transfer Object - Cấu trúc dữ liệu cho request)
    @Data
    static class ChatRequestDTO {
        @NotBlank(message = "Tin nhắn không được để trống")
        private String message;
    }

    // (DTO: Cấu trúc dữ liệu cho response)
    @Data
    @AllArgsConstructor // Tạo constructor (hàm khởi tạo) nhận 1 tham số
    static class ChatResponseDTO {
        private String response;
    }
}
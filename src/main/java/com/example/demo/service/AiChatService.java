package com.example.demo.service;

import com.example.demo.dto.CommandRequestDTO;
import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.model.request.AiRequest;
import com.example.demo.repository.DeviceRepository; // Cần để lấy location
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Service
@Slf4j
//@RequiredArgsConstructor
public class AiChatService {

    // Các service nội bộ của Java
    private final DeviceStateService deviceStateService; // Lấy context vườn (Redis)
    private final WeatherService weatherService;         // Lấy context thời tiết (API)
    private final CommandService commandService;         // Thực thi lệnh (MQTT)
    private final NotificationService notificationService;

    // Client để gọi Python
    private final RestClient restClient;
    private final String pythonApiUrl;

    // Tiêm (Inject) các service và cấu hình
    public AiChatService(DeviceStateService deviceStateService,
                         WeatherService weatherService,
                         CommandService commandService, NotificationService notificationService,
                         RestClient.Builder restClientBuilder,
                         @Value("${ai.service-url}") String pythonApiUrl) {
        this.deviceStateService = deviceStateService;
        this.weatherService = weatherService;
        this.commandService = commandService;
        this.notificationService = notificationService;
        this.restClient = restClientBuilder.build();
        this.pythonApiUrl = pythonApiUrl;
    }

    /**
     * Xử lý chat, tổng hợp bối cảnh và gọi Python AI Service
     */
    public String getChatResponse(String userMessage, String deviceUid) {

        // 1. THU THẬP BỐI CẢNH (CONTEXT)
        // Bối cảnh 1: Trạng thái vườn (từ Redis)
        DeviceStateDTO gardenContext = deviceStateService.getState(deviceUid);

        // Bối cảnh 2: Thời tiết (từ API)
        // Lấy vị trí (location) đã lưu trong CSDL của device
        String location = "Hanoi,VN"; // Mặc định
        AiRequest requestPayload = new AiRequest(userMessage);


        WeatherService.WeatherForecast weatherContext = weatherService.getForecast(location);

        // 2. TẠO REQUEST GỬI ĐẾN PYTHON
        PythonChatRequest request = new PythonChatRequest(userMessage, deviceUid, gardenContext, weatherContext);

        String targetUrl = pythonApiUrl;
        if (!targetUrl.endsWith("/chat")) {
            targetUrl = targetUrl.endsWith("/") ? targetUrl + "chat" : targetUrl + "/chat";
        }
        // 3. GỌI API PYTHON (FastAPI)
        log.info("Calling Python AI service for device: {}", deviceUid);
        PythonChatResponse response = restClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PythonChatResponse.class);

//        notificationService.broadcastAIMessage(response.getTextContent());
        if (response == null) {
            return "Lỗi: Không nhận được phản hồi từ AI service.";
        }


        // 4. XỬ LÝ PHẢN HỒI TỪ PYTHON
        if ("TOOL_CALL".equals(response.getResponseType())) {
            // TRƯỜNG HỢP 2: AI (Python) yêu cầu Java thực thi
            log.info("AI requested tool call: {}", response.getToolCall().getToolName());
            notificationService.broadcastAIMessage(response.getToolCall().getToolName());
            return executeToolCall(response.getToolCall());

        } else {
            // TRƯỜNG HỢP 1: AI (Python) trả lời bằng text
            log.info("AI requested text response.");
//            notificationService.broadcastAIMessage(response.getTextContent());
            return response.getTextContent();
        }
    }

    public String analysis(String userMessage, String deviceUid) {

        DeviceStateDTO gardenContext = deviceStateService.getState(deviceUid);

        String location = "Hanoi,VN";
        AiRequest requestPayload = new AiRequest(userMessage);


        WeatherService.WeatherForecast weatherContext = weatherService.getForecast(location);

        // 2. TẠO REQUEST GỬI ĐẾN PYTHON
        PythonChatRequest request = new PythonChatRequest(userMessage, deviceUid, gardenContext, weatherContext);

        String targetUrl = pythonApiUrl;
        if (!targetUrl.endsWith("/chat")) {
            targetUrl = targetUrl.endsWith("/") ? targetUrl + "chat" : targetUrl + "/chat";
        }
        // 3. GỌI API PYTHON (FastAPI)
        log.info("Calling Python AI service for device: {}", deviceUid);
        PythonChatResponse response = restClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(PythonChatResponse.class);

        if (response == null) {
            return "Lỗi: Không nhận được phản hồi từ AI service.";
        }

        // 4. XỬ LÝ PHẢN HỒI TỪ PYTHON
        if ("TOOL_CALL".equals(response.getResponseType())) {
            // TRƯỜNG HỢP 2: AI (Python) yêu cầu Java thực thi
            log.info("AI requested tool call: {}", response.getToolCall().getToolName());
            notificationService.broadcastAIMessage(response.getToolCall().getToolName());
            return response.getTextContent();
        } else {
            // TRƯỜNG HỢP 1: AI (Python) trả lời bằng text
            log.info("AI requested text response.");
            return response.getTextContent();
        }
    }



    /**
     * Thực thi lệnh gọi hàm (Tool Call) mà Python AI yêu cầu
     */
    private String executeToolCall(ToolCall toolCall) {
        if ("controlDevice".equals(toolCall.getToolName())) {
            Map<String, Object> args = toolCall.getArguments();
            try {
                // Lấy các tham số từ AI
                String deviceUid = (String) args.get("deviceUid");
                String deviceName = (String) args.get("deviceName");
                boolean turnOn = (Boolean) args.get("turnOn");

                // Chuyển đổi DTO (Python) sang DTO (Java MQTT)
                Map<String , Object> payloadMap = Map.of("status", turnOn ? "ON" : "OFF");

                CommandRequestDTO command = new CommandRequestDTO();
                if ("PUMP".equalsIgnoreCase(deviceName)) {
                    command.setAction("SET_PUMP");
                } else if ("LIGHT".equalsIgnoreCase(deviceName)) {
                    command.setAction("SET_LIGHT"); // Giả sử firmware có hỗ trợ
                } else {
                    return "Lỗi: AI yêu cầu điều khiển thiết bị không xác định: " + deviceName;
                }

                command.setPayload(payloadMap);

                // GỌI MQTT SERVICE (Java)
                commandService.sendCommand(deviceUid, command);

                return "Đã rõ! Tôi đã gửi lệnh " + (turnOn ? "bật" : "tắt") + " " + deviceName + ".";

            } catch (Exception e) {
                log.error("Failed to execute AI tool call", e);
                return "Lỗi khi thực thi lệnh: " + e.getMessage();
            }
        }

        return "Lỗi: AI yêu cầu một công cụ không được hỗ trợ: " + toolCall.getToolName();
    }


    // --- DTOs nội bộ để giao tiếp với Python ---
    // (Khớp với Pydantic models của FastAPI)

    @Data
    static class PythonChatRequest {
        @JsonProperty("user_message")
        private String userMessage;
        @JsonProperty("device_uid")
        private String deviceUid;
        @JsonProperty("garden_context")
        private DeviceStateDTO gardenContext;
        @JsonProperty("weather_context")
        private WeatherService.WeatherForecast weatherContext;

        public PythonChatRequest(String userMessage, String deviceUid, DeviceStateDTO gardenContext, WeatherService.WeatherForecast weatherContext) {
            this.userMessage = userMessage;
            this.deviceUid = deviceUid;
            this.gardenContext = gardenContext;
            this.weatherContext = weatherContext;
        }
    }

    @Data
    static class PythonChatResponse {
        @JsonProperty("response_type")
        private String responseType;
        @JsonProperty("text_content")
        private String textContent;
        @JsonProperty("tool_call")
        private ToolCall toolCall;
    }

    @Data
    static class ToolCall {
        @JsonProperty("tool_name")
        private String toolName;
        @JsonProperty("arguments")
        private Map<String, Object> arguments;
    }
}
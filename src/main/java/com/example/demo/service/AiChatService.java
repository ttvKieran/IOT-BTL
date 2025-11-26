package com.example.demo.service;

import com.example.demo.dto.CommandRequestDTO;
import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.model.request.AiRequest;
import com.example.demo.repository.DeviceRepository; // Cần để lấy location
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.HashMap;

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
            // Trong MANUAL mode (getChatResponse), CHỈ GỬI EMAIL, KHÔNG EXECUTE
            log.info("AI requested tool call (MANUAL mode - email only): {}", response.getToolCall().getToolName());
            
            // Gửi email thông báo AI muốn bật thiết bị (có đầy đủ context)
            String emailContent = buildEmailContentFromToolCall(response.getToolCall(), gardenContext, weatherContext);
            notificationService.broadcastAIMessage(emailContent);
            
            return "Đã gửi email thông báo: " + emailContent;

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
            return executeToolCall(response.getToolCall());
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
                // Log toàn bộ arguments để debug
                log.info("AI tool call arguments: {}", args);
                
                // Lấy các tham số từ AI
                String deviceUid = (String) args.get("deviceUid");
                String deviceName = (String) args.get("deviceName");
                Object turnOnObj = args.get("turnOn");
                
                if (deviceUid == null || deviceName == null || turnOnObj == null) {
                    log.error("Missing required arguments. deviceUid={}, deviceName={}, turnOn={}", deviceUid, deviceName, turnOnObj);
                    return "Lỗi: AI không cung cấp đầy đủ tham số (deviceUid, deviceName, turnOn)";
                }
                
                boolean turnOn = Boolean.TRUE.equals(turnOnObj);

                // Lấy duration nếu có (AI sẽ gửi durationMinutes khi bật máy bơm)
                Integer durationMinutes = null;
                if (args.containsKey("durationMinutes")) {
                    Object durationObj = args.get("durationMinutes");
                    if (durationObj instanceof Integer) {
                        durationMinutes = (Integer) durationObj;
                    } else if (durationObj instanceof Double) {
                        durationMinutes = ((Double) durationObj).intValue();
                    }
                }

                // Chuyển đổi DTO (Python) sang DTO (Java MQTT)
                // Tạo payload linh động giống ThresholdService
                Map<String, Object> payloadMap = new HashMap<>();
                payloadMap.put("state", turnOn ? "ON" : "OFF");
                
                // Chỉ gửi kèm 'time' nếu là lệnh ON và có thời gian > 0
                // ESP32 nhận 'time' tính bằng GIÂY
                if (turnOn && durationMinutes != null && durationMinutes > 0) {
                    int durationSeconds = durationMinutes * 60;
                    payloadMap.put("time", durationSeconds);
                    log.info("AI decided watering duration: {} minutes ({} seconds)", durationMinutes, durationSeconds);
                }

                CommandRequestDTO command = new CommandRequestDTO();
                if ("PUMP".equalsIgnoreCase(deviceName)) {
                    command.setAction("CONTROL_PUMP");
                } else if ("LIGHT".equalsIgnoreCase(deviceName)) {
                    command.setAction("SET_LIGHT"); // Giả sử firmware có hỗ trợ
                } else {
                    return "Lỗi: AI yêu cầu điều khiển thiết bị không xác định: " + deviceName;
                }

                command.setPayload(payloadMap);

                // GỌI MQTT SERVICE (Java)
                commandService.sendCommand(deviceUid, command);
                
                log.info("✅ Successfully sent {} command to {}", turnOn ? "ON" : "OFF", deviceName);

                return "Đã rõ! Tôi đã gửi lệnh " + (turnOn ? "bật" : "tắt") + " " + deviceName + ".";

            } catch (Exception e) {
                log.error("Failed to execute AI tool call", e);
                return "Lỗi khi thực thi lệnh: " + e.getMessage();
            }
        }

        return "Lỗi: AI yêu cầu một công cụ không được hỗ trợ: " + toolCall.getToolName();
    }

    /**
     * Tạo nội dung email chi tiết từ tool call để gửi cho user
     */
    private String buildEmailContentFromToolCall(ToolCall toolCall, DeviceStateDTO gardenContext, WeatherService.WeatherForecast weatherContext) {
        StringBuilder emailContent = new StringBuilder();
        
        // Header email
        emailContent.append("🌱 THÔNG BÁO TỪ HỆ THỐNG VƯỜN THÔNG MINH\n");
        emailContent.append("=" .repeat(60)).append("\n\n");
        
        if ("controlDevice".equals(toolCall.getToolName())) {
            String deviceName = (String) toolCall.getArguments().get("deviceName");
            Boolean turnOn = (Boolean) toolCall.getArguments().get("turnOn");
            String action = turnOn ? "BẬT" : "TẮT";
            
            emailContent.append("⚠️ CẢNH BÁO: HỆ THỐNG AI PHÁT HIỆN CẦN CAN THIỆP\n\n");
            emailContent.append("📋 THÔNG TIN THIẾT BỊ:\n");
            emailContent.append(String.format("   • Thiết bị: %s\n", deviceName));
            emailContent.append(String.format("   • Hành động khuyến nghị: %s\n", action));
            emailContent.append(String.format("   • Thời gian: %s\n\n", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            
            // Thông tin cảm biến hiện tại
            emailContent.append("📊 TRẠNG THÁI CẢM BIẾN HIỆN TẠI:\n");
            if (gardenContext != null && gardenContext.getSensors() != null) {
                emailContent.append(String.format("   • Nhiệt độ: %.1f°C\n", gardenContext.getSensors().getTemperature()));
                emailContent.append(String.format("   • Độ ẩm không khí: %.1f%%\n", gardenContext.getSensors().getAirHumidity()));
                emailContent.append(String.format("   • Độ ẩm đất: %.1f%%\n", gardenContext.getSensors().getSoilMoisture()));
                emailContent.append(String.format("   • Ánh sáng: %.1f lux\n\n", gardenContext.getSensors().getLight()));
            }
            
            // Thông tin thời tiết
            if (weatherContext != null) {
                emailContent.append("🌤️ THÔNG TIN THỜI TIẾT:\n");
                emailContent.append(String.format("   • Hiện tại: %s, %.1f°C\n", weatherContext.getDescription(), weatherContext.getTemperature()));
                emailContent.append(String.format("   • Độ ẩm không khí: %d%%\n", weatherContext.getHumidity()));
                emailContent.append(String.format("   • Dự báo 3h tới: %s, %.1f°C\n", weatherContext.getNextDescription(), weatherContext.getNextTemperature()));
                if (weatherContext.isRainExpected()) {
                    emailContent.append(String.format("   • ⚠️ Sắp có mưa (lượng mưa dự kiến: %.1fmm)\n\n", weatherContext.getRainAmount()));
                } else {
                    emailContent.append("   • ✅ Không có mưa trong 3 giờ tới\n\n");
                }
            }
            
            // Lý do và hướng dẫn
            emailContent.append("💡 LÝ DO:\n");
            emailContent.append("   Hệ thống AI phân tích dữ liệu cảm biến và thời tiết,\n");
            emailContent.append("   phát hiện cây trồng cần được tưới nước để đảm bảo phát triển tốt.\n\n");
            
            emailContent.append("⚙️ CHẾ ĐỘ HOẠT ĐỘNG:\n");
            emailContent.append("   • Chế độ hiện tại: MANUAL (Thủ công)\n");
            emailContent.append("   • Hệ thống KHÔNG tự động thực hiện hành động\n");
            emailContent.append("   • Yêu cầu xác nhận và thực hiện thủ công\n\n");
            
            emailContent.append("📱 HƯỚNG DẪN:\n");
            emailContent.append("   1. Kiểm tra điều kiện thực tế của vườn\n");
            emailContent.append("   2. Đăng nhập vào ứng dụng để điều khiển thiết bị\n");
            emailContent.append(String.format("   3. %s %s nếu cần thiết\n\n", action, deviceName));
            
            emailContent.append("ℹ️ GHI CHÚ:\n");
            emailContent.append("   Để hệ thống tự động thực hiện, hãy chuyển sang chế độ AUTO\n");
            emailContent.append("   trong cài đặt ứng dụng.\n\n");
            
        } else if ("controlPumpDuration".equals(toolCall.getToolName())) {
            Integer durationMinutes = (Integer) toolCall.getArguments().get("durationMinutes");
            
            emailContent.append("⚠️ CẢNH BÁO: CẦN TƯỚI NƯỚC CHO CÂY TRỒNG\n\n");
            emailContent.append("📋 THÔNG TIN TƯỚI NƯỚC:\n");
            emailContent.append(String.format("   • Hành động: BẬT MÁY BỠM TƯỚI NƯỚC\n"));
            emailContent.append(String.format("   • Thời gian đề xuất: %d phút\n", durationMinutes));
            emailContent.append(String.format("   • Thời điểm: %s\n\n", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            
            // Thông tin cảm biến hiện tại
            emailContent.append("📊 TRẠNG THÁI CẢM BIẾN HIỆN TẠI:\n");
            if (gardenContext != null && gardenContext.getSensors() != null) {
                emailContent.append(String.format("   • Nhiệt độ: %.1f°C\n", gardenContext.getSensors().getTemperature()));
                emailContent.append(String.format("   • Độ ẩm không khí: %.1f%%\n", gardenContext.getSensors().getAirHumidity()));
                emailContent.append(String.format("   • Độ ẩm đất: %.1f%% ⚠️\n", gardenContext.getSensors().getSoilMoisture()));
                emailContent.append(String.format("   • Ánh sáng: %.1f lux\n\n", gardenContext.getSensors().getLight()));
            }
            
            // Thông tin thời tiết
            if (weatherContext != null) {
                emailContent.append("🌤️ THÔNG TIN THỜI TIẾT:\n");
                emailContent.append(String.format("   • Hiện tại: %s, %.1f°C\n", weatherContext.getDescription(), weatherContext.getTemperature()));
                emailContent.append(String.format("   • Độ ẩm không khí: %d%%\n", weatherContext.getHumidity()));
                emailContent.append(String.format("   • Dự báo 3h tới: %s, %.1f°C\n", weatherContext.getNextDescription(), weatherContext.getNextTemperature()));
                if (weatherContext.isRainExpected()) {
                    emailContent.append(String.format("   • ⚠️ Sắp có mưa (lượng mưa dự kiến: %.1fmm)\n\n", weatherContext.getRainAmount()));
                } else {
                    emailContent.append("   • ✅ Không có mưa trong 3 giờ tới\n\n");
                }
            }
            
            // Lý do và hướng dẫn
            emailContent.append("💡 PHÂN TÍCH CỦA AI:\n");
            emailContent.append("   • Độ ẩm đất thấp hơn ngưỡng tối ưu\n");
            emailContent.append("   • Điều kiện thời tiết không có mưa trong thời gian tới\n");
            emailContent.append("   • Cây trồng cần được bổ sung nước để phát triển\n\n");
            
            emailContent.append("⚙️ CHẾ ĐỘ HOẠT ĐỘNG:\n");
            emailContent.append("   • Chế độ hiện tại: MANUAL (Thủ công)\n");
            emailContent.append("   • Hệ thống KHÔNG tự động bật máy bơm\n");
            emailContent.append("   • Yêu cầu xác nhận từ người dùng\n\n");
            
            emailContent.append("📱 HƯỚNG DẪN THỰC HIỆN:\n");
            emailContent.append("   1. Kiểm tra trực tiếp độ ẩm đất tại vườn\n");
            emailContent.append("   2. Đăng nhập vào ứng dụng Smart Garden\n");
            emailContent.append("   3. Vào phần Điều khiển > Máy bơm\n");
            emailContent.append(String.format("   4. Bật máy bơm và đặt timer %d phút\n\n", durationMinutes));
            
            emailContent.append("⏰ LƯU Ý:\n");
            emailContent.append(String.format("   • Nên tưới vào buổi sáng sớm hoặc chiều mát\n"));
            emailContent.append(String.format("   • Kiểm tra lại sau %d phút để tránh tưới quá nhiều\n", durationMinutes));
            emailContent.append("   • Để tự động hóa, chuyển sang chế độ AUTO\n\n");
        } else {
            emailContent.append("⚠️ THÔNG BÁO TỪ HỆ THỐNG AI\n\n");
            emailContent.append(String.format("   AI yêu cầu thực hiện: %s\n\n", toolCall.getToolName()));
        }
        
        // Footer
        emailContent.append("─".repeat(60)).append("\n");
        emailContent.append("📧 Email tự động từ Smart Garden System\n");
        emailContent.append("🔗 Truy cập: http://localhost:3000\n");
        emailContent.append("⚙️ Cài đặt thông báo tại mục Settings trong ứng dụng\n");
        
        return emailContent.toString();
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
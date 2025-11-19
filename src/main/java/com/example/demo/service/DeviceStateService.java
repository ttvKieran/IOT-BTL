package com.example.demo.service;

import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.model.entity.TelemetryLog;
import com.example.demo.repository.TelemetryRepository;
import com.example.demo.utils.mapper.TelemetryMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceStateService {
    private final RedisTemplate<String, DeviceStateDTO> deviceStateRedisTemplate;
    private final ObjectMapper objectMapper; // Dùng để parse JSON payload từ MQTT
    TelemetryRepository telemetryRepository;
    TelemetryMapper telemetryMapper;

    // Prefix cho Key Redis
    private static final String REDIS_KEY_PREFIX = "device:state:";

    // Phương thức tạo Key cho Redis
    private String createKey(String deviceUid) {
        return REDIS_KEY_PREFIX + deviceUid;
    }

    /**
     * 1. Lấy trạng thái tức thời của thiết bị từ Redis.
     */
    public DeviceStateDTO getState(String deviceUid) {
        String key = createKey(deviceUid);
        DeviceStateDTO state = deviceStateRedisTemplate.opsForValue().get(key);

        // Nếu không có trạng thái trong Redis, trả về trạng thai mặc định
        if (state == null) {
            state = new DeviceStateDTO();
            state.setDeviceUid(deviceUid);
            state.setStatus("offline");
            state.setSensors(new DeviceStateDTO.SensorData());
            log.debug("No existing state in Redis for deviceUid={}. Returning default state: {}", deviceUid, state);
            return state;
        }

        log.debug("Retrieved device state from Redis for deviceUid={}: {}", deviceUid, state);
        return state;
    }

    public DeviceStateDTO updateStateFromMqtt(String deviceUid, String messageType, String payload) {
        String key = createKey(deviceUid);
        DeviceStateDTO currentState = getState(deviceUid);

        try {
            switch (messageType.toLowerCase()) {
                case "telemetry":
                    try {
                        // 1. Đọc payload thành một cây JSON chung
                        JsonNode rootNode = objectMapper.readTree(payload);

                        // 2. Lấy nút (node) con có tên là "sensors"
                        JsonNode sensorsNode = rootNode.get("sensors");

                        if (sensorsNode != null) {
                            // 3. CHỈ chuyển đổi nút "sensors" thành đối tượng SensorData
                            DeviceStateDTO.SensorData sensorData = objectMapper.treeToValue(sensorsNode, DeviceStateDTO.SensorData.class);

                            // 4. Cập nhật trạng thái
                            currentState.setSensors(sensorData);
                            currentState.setLastSeen(System.currentTimeMillis());

                        } else {
                            // Ghi log nếu payload không có đối tượng "sensors"
                            log.warn("Telemetry payload không chứa đối tượng 'sensors': {}", payload);
                        }

                    } catch (Exception e) {
                        log.error("Lỗi parse JSON cho 'telemetry': {}", e.getMessage());
                    }
                    break;
                case "status":
                    try {
                        // Dùng readValue để parse JSON, ví dụ: {"status": "online"}
                        DeviceStateDTO statusUpdate = objectMapper.readValue(payload, DeviceStateDTO.class);
                        if (statusUpdate.getStatus() != null) {
                            currentState.setStatus(statusUpdate.getStatus());
                        }
                    } catch (Exception e) {
                        // Fallback nếu payload là String thô "online" (như code cũ)
                        log.warn("Payload 'status' không phải JSON, sử dụng payload thô. Payload: {}", payload);
                        currentState.setStatus(payload);
                    }
                    currentState.setLastSeen(System.currentTimeMillis());
                    break;
                case "state":
                    // Giả định payload là JSON: {"control_mode": "AUTO", "pump_state": "OFF"}
                    // ObjectMapper (nếu là Bean của Spring) sẽ tự động xử lý
                    // snake_case (control_mode) sang camelCase (controlMode).

                    // Sử dụng readerForUpdating để "merge" payload vào đối tượng currentState
                    // mà không làm null các trường khác (như sensors, status).
                    objectMapper.readerForUpdating(currentState).readValue(payload);

                    currentState.setLastSeen(System.currentTimeMillis());
                    log.debug("Processed 'state' update for deviceUid={}", deviceUid);
                    break;
                // Xử lý các loại messageType khác nếu cần
                default:
                    log.warn("Unknown message type: {}", messageType);
            }

            // Lưu trạng thái cập nhật trở lại Redis
            deviceStateRedisTemplate.opsForValue().set(key, currentState);
            log.debug("Updated device state in Redis for deviceUid={}: {}", deviceUid, currentState);

        } catch (Exception e) {
            log.error("Failed to update device state from MQTT for deviceUid={}: {}", deviceUid, e.getMessage());
        }

        return currentState;
    }

}
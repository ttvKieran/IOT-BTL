package com.example.demo.dto;// src/main/java/com/example/smartgarden/dto/DeviceStateDTO.java


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStateDTO implements Serializable {

    // IMPORTANT: Cần Serializable cho Redis/Cache
    private static final long serialVersionUID = 1L;

    private String deviceUid;
    private String status; // "ONLINE" | "OFFLINE"
    private long lastSeen; // Timestamp (Epoch Millis)
    private String controlMode; // "AUTO" | "MANUAL"
    private String pumpState; // "ON" | "OFF"
    private SensorData sensors; // Dữ liệu cảm biến

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorData implements Serializable {
        private static final long serialVersionUID = 1L;
        private double temperature;
        private double airHumidity;
        private double light; // Mức ánh sáng (ví dụ: Lux)
        private double soilMoisture; // Độ ẩm đất (%)
    }
}
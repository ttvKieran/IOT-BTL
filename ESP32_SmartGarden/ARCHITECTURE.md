# Sơ đồ kiến trúc hệ thống Smart Garden

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ESP32 SMART GARDEN                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                │
│  │   DHT11      │     │   BH1750     │     │ Soil Sensor  │                │
│  │ (Temp + Hum) │     │   (Light)    │     │  (Moisture)  │                │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘                │
│         │                    │                     │                         │
│         │ GPIO4         I2C  │ SDA/SCL        ADC  │ GPIO34                 │
│         │                    │                     │                         │
│  ┌──────┴────────────────────┴─────────────────────┴───────┐                │
│  │                                                           │                │
│  │                      ESP32 DevKit                        │                │
│  │                                                           │                │
│  │  • Đọc cảm biến định kỳ (10s)                            │                │
│  │  • Gửi telemetry qua MQTT                                │                │
│  │  • Nhận lệnh điều khiển                                  │                │
│  │  • Điều khiển máy bơm                                    │                │
│  │                                                           │                │
│  └───────────────────────────┬───────────────────────────────┘                │
│                              │ GPIO5                                         │
│                              │                                               │
│                     ┌────────┴────────┐                                      │
│                     │  Relay Module   │                                      │
│                     └────────┬────────┘                                      │
│                              │                                               │
│                     ┌────────┴────────┐                                      │
│                     │   Water Pump    │                                      │
│                     │   (Máy bơm)     │                                      │
│                     └─────────────────┘                                      │
│                                                                              │
└────────────────────────────────┬─────────────────────────────────────────────┘
                                 │
                                 │ WiFi
                                 │
┌────────────────────────────────┴─────────────────────────────────────────────┐
│                          MQTT BROKER (Mosquitto)                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Topics:                                                                     │
│  • smartgarden/device/{deviceUid}/telemetry    (ESP32 → Server)             │
│  • smartgarden/device/{deviceUid}/status       (ESP32 → Server)             │
│  • smartgarden/device/{deviceUid}/state        (ESP32 → Server)             │
│  • smartgarden/device/{deviceUid}/command      (Server → ESP32)             │
│                                                                              │
│  Port: 18883 (Docker) / 1883 (Local)                                        │
│  Auth: iot_admin / 123456                                                   │
│                                                                              │
└────────────────────────────────┬─────────────────────────────────────────────┘
                                 │
                                 │ MQTT Protocol
                                 │
┌────────────────────────────────┴─────────────────────────────────────────────┐
│                         SPRING BOOT BACKEND                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────┐            │
│  │              MqttProcessingService                          │            │
│  │  • Nhận dữ liệu từ MQTT (telemetry, status, state)          │            │
│  │  • Parse JSON                                               │            │
│  │  • Cập nhật Redis Cache                                     │            │
│  └────────────────┬────────────────────────────────────────────┘            │
│                   │                                                          │
│  ┌────────────────┴────────────────────────────────────────────┐            │
│  │                 DeviceStateService                          │            │
│  │  • Quản lý trạng thái thiết bị trong Redis                  │            │
│  │  • Update state theo từng message type                      │            │
│  └─────────────────────────────────────────────────────────────┘            │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │                 TelemetryService                             │           │
│  │  • Lưu lịch sử dữ liệu cảm biến vào MySQL (async)            │           │
│  └──────────────────────────────────────────────────────────────┘           │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │                 CommandService                               │           │
│  │  • Gửi lệnh điều khiển xuống ESP32 qua MQTT                  │           │
│  │  • Actions: CONTROL_PUMP, SET_MODE, REQUEST_STATE            │           │
│  └──────────────────────────────────────────────────────────────┘           │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │              NotificationService                             │           │
│  │  • Broadcast cập nhật qua WebSocket                          │           │
│  │  • Push notification cho Web/Mobile                          │           │
│  └──────────────────────────────────────────────────────────────┘           │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────┐           │
│  │                REST API Controllers                          │           │
│  │  • /api/devices - Quản lý thiết bị                           │           │
│  │  • /api/devices/{id}/command - Gửi lệnh                      │           │
│  │  • /api/devices/{id}/telemetry - Lấy lịch sử                │           │
│  │  • /api/ai/chat - Tích hợp AI                                │           │
│  └──────────────────────────────────────────────────────────────┘           │
│                                                                              │
└──────────────┬──────────────────┬──────────────────┬────────────────────────┘
               │                  │                  │
               │                  │                  │
      ┌────────┴────────┐  ┌──────┴─────┐  ┌────────┴──────┐
      │  MySQL Database │  │   Redis    │  │   WebSocket   │
      │  (Telemetry Log)│  │  (Cache)   │  │ (Real-time)   │
      └─────────────────┘  └────────────┘  └───────┬───────┘
                                                    │
                                                    │
                                          ┌─────────┴─────────┐
                                          │  Web Frontend     │
                                          │  • Dashboard      │
                                          │  • Control Panel  │
                                          │  • Charts         │
                                          └───────────────────┘
```

## Luồng dữ liệu

### 1. Telemetry (Dữ liệu cảm biến)
```
ESP32 (Sensors) 
  → Read DHT11, BH1750, Soil Sensor (mỗi 10s)
  → Publish JSON to: smartgarden/device/{deviceUid}/telemetry
  → MQTT Broker
  → Spring Boot (MqttProcessingService)
  → Update Redis Cache (DeviceStateService)
  → Save to MySQL (TelemetryService - async)
  → WebSocket Broadcast (NotificationService)
  → Web Dashboard (Real-time update)
```

### 2. Control (Điều khiển từ Server)
```
Web Dashboard (User clicks "Turn ON Pump")
  → HTTP POST /api/devices/{id}/command
  → DeviceController
  → CommandService
  → Publish JSON to: smartgarden/device/{deviceUid}/command
  → MQTT Broker
  → ESP32 receives command
  → Execute action (digitalWrite PUMP_PIN)
  → Publish state update to: smartgarden/device/{deviceUid}/state
  → Backend updates Redis & broadcast via WebSocket
  → Web Dashboard shows updated state
```

### 3. Heartbeat (Keep-alive)
```
ESP32 (mỗi 5s)
  → Publish to: smartgarden/device/{deviceUid}/status
  → Payload: {"status": "ONLINE", "timestamp": ...}
  → Backend updates lastSeen in Redis
  → WebSocket broadcast device status
```

## Cấu trúc dữ liệu

### Redis Cache (DeviceStateDTO)
```json
{
  "deviceUid": "ESP32_GARDEN_001",
  "status": "ONLINE",
  "lastSeen": 1700123456789,
  "controlMode": "MANUAL",
  "pumpState": "OFF",
  "sensors": {
    "temperature": 28.5,
    "airHumidity": 65.2,
    "light": 450.5,
    "soilMoisture": 45.0
  }
}
```

### MySQL (TelemetryLog)
```sql
CREATE TABLE telemetry_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  device_id BIGINT NOT NULL,
  temperature DOUBLE,
  air_humidity DOUBLE,
  light_level DOUBLE,
  soil_moisture DOUBLE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (device_id) REFERENCES device_entity(id)
);
```

## Ports và Endpoints

| Service | Port | Protocol | Endpoint |
|---------|------|----------|----------|
| Spring Boot Backend | 8080 | HTTP | http://localhost:8080 |
| MQTT Broker (Docker) | 18883 | MQTT | tcp://localhost:18883 |
| MQTT Broker (Local) | 1883 | MQTT | tcp://localhost:1883 |
| MySQL Database | 13306 | MySQL | localhost:13306 |
| Redis Cache | 6379 | Redis | localhost:6379 |
| AI Service | 8000 | HTTP | http://localhost:8000 |

## MQTT Topics Pattern

```
smartgarden/
├── device/
│   ├── {deviceUid}/
│   │   ├── telemetry    (ESP32 → Server) - Sensor data
│   │   ├── status       (ESP32 → Server) - Online/Offline
│   │   ├── state        (ESP32 → Server) - Full device state
│   │   └── command      (Server → ESP32) - Control commands
```

## Security

- MQTT Authentication: Username/Password (iot_admin/123456)
- Spring Boot: Có thể tích hợp Spring Security
- WebSocket: Token-based authentication
- HTTPS: Nên enable cho production

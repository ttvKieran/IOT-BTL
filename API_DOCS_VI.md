# Tài liệu API — Smart Garden (Synthia) (Tiếng Việt)

Tài liệu này được chuyển từ OpenAPI (v3) JSON cung cấp. Tất cả endpoint đều mặc định có server: `http://localhost:8080`.

---

## Tổng quan các endpoint

- Device Controller (quản lý thiết bị)
  - GET  /api/v1/devices
  - POST /api/v1/devices
  - PUT  /api/v1/devices/{deviceUid}
  - DELETE /api/v1/devices/{deviceUid}
  - POST /api/v1/devices/{deviceUid}/restore
  - POST /api/v1/devices/{deviceUid}/command
  - GET /api/v1/devices/{deviceUid}/state
  - GET /api/v1/devices/{deviceUid}/history?from={iso}&to={iso}

- AI Chat
  - POST /api/v1/ai/chat/{deviceUid}

---

## Header bảo mật

---

## Schemas chính (tóm tắt)

- DeviceDto
  - id: integer (int64)
  - createdAt, updatedAt, deletedAt: string (date-time)
  - device_uid: string (UID thiết bị, ví dụ `ESP32_GARDEN_01`)
  - name: string (tên thiết bị)

- CommandRequestDTO
  - action (required): string — tên hành động (ví dụ: `TURN_ON_PUMP`)
  - payload: object — tùy biến theo action (object động)

- ChatRequestDTO
  - message (required): string — nội dung chat gửi tới AI

- ChatResponseDTO
  - response: string — văn bản phản hồi từ AI

- DeviceStateDTO
  - deviceUid: string
  - status: string
  - lastSeen: integer (timestamp int64)
  - controlMode: string
  - pumpState: string
  - sensors: SensorData

- SensorData
  - temperature: number (°C)
  - airHumidity: number (%)
  - light: number (lux)
  - soilMoisture: number (%)

- TelemetryLogDto
  - id: integer
  - device: DeviceDto
  - logTime: date-time
  - temperature, airHumidity, lightLevel, soilMoisture: numbers

- ApiResponse... (wrapper)
  - code: integer
  - message: string
  - data: (tùy endpoint — DeviceDto, list, DeviceStateDTO, v.v.)

---

## Endpoint chi tiết

### 1) Lấy danh sách các thiết bị
- Method: GET
- URL: `/api/v1/devices`
- Auth: Bearer token
- Query: (không có)
- Request body: none
- Response 200: ApiResponseListDeviceDto
  - data: array of DeviceDto

Ví dụ (curl):

```bash
curl http://localhost:8080/api/v1/devices
```

Response (ví dụ):

```json
{
  "code": 200,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "device_uid": "ESP32_GARDEN_01",
      "name": "Máy bơm vườn sau",
      "createdAt": "2025-11-12T08:00:00Z"
    },
    {
      "id": 2,
      "device_uid": "ESP32_GARDEN_02",
      "name": "Cảm biến đất",
      "createdAt": "2025-11-12T09:00:00Z"
    }
  ]
}
```

---

### 2) Tạo thiết bị mới
- Method: POST
- URL: `/api/v1/devices`
- Auth: Bearer token
- Request body (application/json): DeviceDto (fields: device_uid, name, ...)

Ví dụ body:

```json
{
  "device_uid": "ESP32_GARDEN_02",
  "name": "Máy bơm vườn trước"
}
```

- Response 200: ApiResponseDeviceDto (data = DeviceDto)

Ví dụ request + response:

Request (curl):

```bash
curl -X POST http://localhost:8080/api/v1/devices \
  -H "Content-Type: application/json" \
  -d '{"device_uid":"ESP32_GARDEN_02","name":"Máy bơm vườn trước"}'
```

Response (ví dụ):

```json
{
  "code": 200,
  "message": "Created",
  "data": {
    "id": 3,
    "device_uid": "ESP32_GARDEN_02",
    "name": "Máy bơm vườn trước",
    "createdAt": "2025-11-13T10:00:00Z"
  }
}
```
---

### 3) Cập nhật thông tin thiết bị
- Method: PUT
- URL: `/api/v1/devices/{deviceUid}`
- Path params:
  - deviceUid (string) — UID của thiết bị
- Request body: DeviceDto (JSON) — trường có thể gồm `name` hoặc các thuộc tính khác
- Response 200: ApiResponseDeviceDto

Ví dụ curl:

```bash
curl -X PUT -H "Content-Type: application/json" \
  -d '{"name":"Máy bơm sau - updated"}' \
  http://localhost:8080/api/v1/devices/ESP32_GARDEN_01
```

Response (ví dụ):

```json
{
  "code": 200,
  "message": "Updated",
  "data": {
    "id": 1,
    "device_uid": "ESP32_GARDEN_01",
    "name": "Máy bơm sau - updated",
    "updatedAt": "2025-11-13T11:00:00Z"
  }
}
```

---

### 4) Xóa mềm thiết bị (soft delete)
- Method: DELETE
- URL: `/api/v1/devices/{deviceUid}`
- Path params: deviceUid
- Response 200: ApiResponseVoid

Ví dụ request + response:

Request (curl):

```bash
curl -X DELETE http://localhost:8080/api/v1/devices/ESP32_GARDEN_02
```

Response (ví dụ):

```json
{
  "code": 200,
  "message": "Deleted",
  "data": {}
}
```

---

### 5) Khôi phục thiết bị đã xóa mềm
- Method: POST
- URL: `/api/v1/devices/{deviceUid}/restore`
- Path params: deviceUid
- Response 200: ApiResponseVoid

Ví dụ request + response:

Request (curl):

```bash
curl -X POST http://localhost:8080/api/v1/devices/ESP32_GARDEN_02/restore
```

Response (ví dụ):

```json
{
  "code": 200,
  "message": "Restored",
  "data": {}
}
```

---

### 6) Gửi lệnh điều khiển xuống thiết bị (qua MQTT Outbound)
- Method: POST
- URL: `/api/v1/devices/{deviceUid}/command`
- Path params: deviceUid
- Request body (application/json): CommandRequestDTO
  - action (required): string
  - payload: object (tùy action)

Ví dụ body:

```json
{
  "action": "SET_PUMP",
  "payload": { "state": "ON" }
}
```

- Response 200: ApiResponseVoid

Lưu ý: Backend sẽ gửi message tới MQTT topic tương ứng cho thiết bị.

Ví dụ request (curl) và response:

```bash
curl -X POST http://localhost:8080/api/v1/devices/ESP32_GARDEN_01/command \
  -H "Content-Type: application/json" \
  -d '{"action":"SET_PUMP","payload":{"state":"ON"}}'
```

Ví dụ: backend forward payload MQTT tới topic `devices/ESP32_GARDEN_01/commands`:

MQTT message (payload JSON):

```json
{
  "action": "SET_PUMP",
  "payload": { "state": "ON" },
  "sentBy": "backend",
  "timestamp": "2025-11-13T11:05:00Z"
}
```

Response HTTP (ví dụ):

```json
{
  "code": 200,
  "message": "Command forwarded",
  "data": {}
}
```

---

### 7) Gửi tin nhắn chat đến AI của vườn
- Method: POST
- URL: `/api/v1/ai/chat/{deviceUid}`
- Path params: deviceUid
- Request body: ChatRequestDTO
  - message (required): string
- Response 200: ChatResponseDTO
  - response: string — văn bản trả về từ AI

Ví dụ body:

```json
{ "message": "Bật bơm giúp tôi" }
```

Ví dụ request + response:

Request (curl):

```bash
curl -X POST http://localhost:8080/api/v1/ai/chat/ESP32_GARDEN_01 \
  -H "Content-Type: application/json" \
  -d '{"message":"Bật bơm giúp tôi"}'
```

Response (ví dụ):

```json
{
  "response": "Đã bật bơm. Tôi sẽ theo dõi trạng thái và thông báo nếu có vấn đề."
}
```

---

### 8) Lấy trạng thái tức thời của thiết bị (từ Redis cache)
- Method: GET
- URL: `/api/v1/devices/{deviceUid}/state`
- Path params: deviceUid
- Response 200: ApiResponseDeviceStateDTO
  - data: DeviceStateDTO

DeviceStateDTO ví dụ:

```json
{
  "deviceUid": "ESP32_GARDEN_01",
  "status": "online",
  "lastSeen": 1699999999000,
  "controlMode": "auto",
  "pumpState": "OFF",
  "sensors": {
    "temperature": 28.5,
    "airHumidity": 60.0,
    "light": 1200.5,
    "soilMoisture": 45.75
  }
}
```

---

### 9) Lấy lịch sử dữ liệu cảm biến trong khoảng thời gian
- Method: GET
- URL: `/api/v1/devices/{deviceUid}/history`
- Path params:
  - deviceUid (string)
- Query params (required):
  - from (string, date-time) — thời điểm bắt đầu (ISO)
  - to (string, date-time) — thời điểm kết thúc (ISO)

Ví dụ:

```
GET /api/v1/devices/ESP32_GARDEN_01/history?from=2025-01-01T00:00:00Z&to=2025-01-02T00:00:00Z
```

Response 200: ApiResponseListTelemetryLogDto
- data: array of TelemetryLogDto

TelemetryLogDto ví dụ:

```json
{
  "id": 1001,
  "device": { "device_uid": "ESP32_GARDEN_01", "name": "Máy bơm" },
  "logTime": "2025-11-12T08:00:00Z",
  "temperature": 28.5,
  "airHumidity": 60.0,
  "lightLevel": 1200.5,
  "soilMoisture": 45.75
}
```

---

## Ví dụ dùng `curl` (tổng hợp)

1) Tạo thiết bị

```bash
curl -X POST http://localhost:8080/api/v1/devices \
  -H "Content-Type: application/json" \
  -d '{"device_uid":"ESP32_GARDEN_03","name":"Máy bơm vườn trái"}'
```

2) Gửi lệnh bật bơm

```bash
curl -X POST http://localhost:8080/api/v1/devices/ESP32_GARDEN_01/command \
  -H "Content-Type: application/json" \
  -d '{"action":"SET_PUMP","payload":{"state":"ON"}}'
```

---

## WebSocket (STOMP) topics

Hệ thống sử dụng Spring WebSocket (SimpMessagingTemplate) để broadcast các cập nhật realtime.

- ` /topic/device/{deviceUid}`
  - Mô tả: gửi trạng thái tức thời của một thiết bị đến các client đã subscribe.
  - Payload: `DeviceStateDTO` (JSON). Ví dụ:

```json
{
  "deviceUid": "ESP32_GARDEN_01",
  "status": "online",
  "lastSeen": 1699999999000,
  "controlMode": "auto",
  "pumpState": "OFF",
  "sensors": {
    "temperature": 28.5,
    "airHumidity": 60.0,
    "light": 1200.5,
    "soilMoisture": 45.75
  }
}
```

- `/topic/ai/chat`
  - Mô tả: gửi thông báo/nhắn tin từ AI tới các client (ví dụ admin dashboard).
  - Payload: string (hoặc object nếu cần). Ví dụ:

```json
"Synthia: Đã bật bơm cho thiết bị ESP32_GARDEN_01"
```

3) Gọi AI chat

```bash
curl -X POST http://localhost:8080/api/v1/ai/chat/ESP32_GARDEN_01 \
  -H "Content-Type: application/json" \
  -d '{"message":"Kiểm tra độ ẩm đất"}'
```

---

## Ghi chú & Best practices

- Mã trả về (ApiResponse) luôn có `code` và `message`. Kiểm tra `code` để xử lý lỗi rõ ràng.
- Khi gọi endpoint `command`, backend chỉ forward lệnh qua MQTT — đảm bảo thiết bị đang subscribe đúng topic.
- Tất cả timestamp định dạng ISO (date-time) hoặc integer cho một vài field (`lastSeen`).
- Khi test micro / AI, nhớ rằng AI service có thể trả về JSON "tool call" để backend thực hiện hành động; backend cần validate trước khi thực hiện.



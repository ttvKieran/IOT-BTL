# 🎉 Hệ thống Smart Garden đã hoàn thiện!

## ✅ Các tính năng đã hoàn thành

### 1. 📊 **Dashboard** - Biểu đồ thống kê
- Xem dữ liệu lịch sử từ database (MySQL)
- Chọn xem theo **1 ngày** hoặc **1 tuần**
- 4 thống kê trung bình: Nhiệt độ, Độ ẩm KK, Ánh sáng, Độ ẩm đất
- 4 biểu đồ line chart đẹp mắt (Chart.js)
- Responsive design

### 2. 🎛️ **Control Panel** - Điều khiển real-time
- Hiển thị dữ liệu cảm biến **REAL-TIME** qua WebSocket
- Điều khiển máy bơm (ON/OFF) bằng toggle switch
- Chuyển chế độ: MANUAL / AUTO
- Hiển thị trạng thái thiết bị (Online/Offline)
- Progress bar cho mỗi cảm biến
- Auto-refresh không cần tải lại trang

### 3. 🤖 **AI Chat** - Trợ lý thông minh
- Chat với AI Synthia (Gemini)
- Điều khiển bằng giọng nói (Speech-to-Text)
- Hỏi về trạng thái vườn bằng ngôn ngữ tự nhiên

### 4. 🏠 **Landing Page**
- Giới thiệu hệ thống
- Trạng thái thiết bị
- Link nhanh đến các trang

---

## 🌐 Truy cập các trang

| Trang | URL | Mô tả |
|-------|-----|-------|
| 🏠 **Trang chủ** | http://localhost:8080/home.html | Landing page |
| 📊 **Dashboard** | http://localhost:8080/dashboard.html | Biểu đồ thống kê lịch sử |
| 🎛️ **Control Panel** | http://localhost:8080/control.html | Điều khiển real-time |
| 🤖 **AI Chat** | http://localhost:8080/index.html | Trợ lý AI Synthia |
| 📖 **API Docs** | http://localhost:8080/swagger-ui.html | API Documentation |

---

## 🚀 Quick Start

### 1. Khởi động hệ thống (Docker)

```powershell
cd "d:\Year4_Semester 1\IoT\BTL\Template"
docker-compose up -d
```

### 2. Kiểm tra trạng thái

```powershell
docker-compose ps
```

Tất cả containers phải **Up** (healthy):
- ✅ mysql_db
- ✅ redis_cache
- ✅ mqtt_broker
- ✅ smart_garden_app
- ✅ python-ai

### 3. Đăng ký thiết bị

**Option A: Qua Swagger UI**
1. Mở: http://localhost:8080/swagger-ui.html
2. **Device Controller** → **POST /api/v1/devices**
3. Nhập:
```json
{
  "deviceUid": "ESP32_GARDEN_001",
  "name": "Vườn tầng thượng"
}
```

**Option B: Qua PowerShell**
```powershell
$body = @{
    deviceUid = "ESP32_GARDEN_001"
    name = "Vườn tầng thượng"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/devices" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### 4. Giả lập ESP32 gửi dữ liệu (Test)

**Sử dụng MQTTX:**

1. Download MQTTX: https://mqttx.app/
2. Kết nối:
   - Host: `localhost`
   - Port: `1883`
   - Username: `iot_admin`
   - Password: `123456`

3. Gửi telemetry:
```
Topic: smartgarden/device/ESP32_GARDEN_001/telemetry

Payload:
{
  "sensors": {
    "temperature": 28.5,
    "airHumidity": 65.0,
    "light": 450.0,
    "soilMoisture": 45.0
  }
}
```

4. Gửi status:
```
Topic: smartgarden/device/ESP32_GARDEN_001/status
Retained: ✅ (bật)

Payload:
{
  "status": "ONLINE"
}
```

5. Gửi state:
```
Topic: smartgarden/device/ESP32_GARDEN_001/state
Retained: ✅ (bật)

Payload:
{
  "controlMode": "MANUAL",
  "pumpState": "OFF"
}
```

### 5. Xem kết quả

**Dashboard:**
1. Mở http://localhost:8080/dashboard.html
2. Chọn thiết bị: ESP32_GARDEN_001
3. Chọn "1 Ngày"
4. Sẽ thấy biểu đồ với dữ liệu vừa gửi

**Control Panel:**
1. Mở http://localhost:8080/control.html
2. Chọn thiết bị: ESP32_GARDEN_001
3. Sẽ thấy:
   - Trạng thái: **Trực tuyến** (màu xanh)
   - Dữ liệu cảm biến real-time
   - Toggle máy bơm
   - Nút MANUAL/AUTO

**Test điều khiển:**
1. Click toggle máy bơm → **ON**
2. Mở MQTTX, subscribe topic:
   ```
   smartgarden/device/ESP32_GARDEN_001/command
   ```
3. Sẽ thấy message:
   ```json
   {
     "action": "CONTROL_PUMP",
     "payload": {
       "state": "ON"
     }
   }
   ```

---

## 🎯 Demo Scenarios

### Scenario 1: Xem lịch sử dữ liệu

1. **Gửi nhiều telemetry** (dùng MQTTX):
   - Gửi 10-20 message với giá trị khác nhau
   - Đợi mỗi message 5-10 giây

2. **Xem Dashboard**:
   - Mở http://localhost:8080/dashboard.html
   - Chọn "1 Ngày"
   - Sẽ thấy biểu đồ line chart với nhiều điểm dữ liệu

### Scenario 2: Điều khiển real-time

1. **Mở Control Panel**: http://localhost:8080/control.html

2. **Mở MQTTX**, subscribe:
   ```
   smartgarden/device/ESP32_GARDEN_001/command
   ```

3. **Test các lệnh:**
   - Click toggle máy bơm → Thấy command trong MQTTX
   - Click nút MANUAL → Thấy command SET_MODE
   - Click nút AUTO → Thấy command SET_MODE

4. **Giả lập ESP32 phản hồi:**
   - Sau khi nhận command, gửi state update:
   ```
   Topic: smartgarden/device/ESP32_GARDEN_001/state
   
   Payload:
   {
     "controlMode": "AUTO",
     "pumpState": "ON"
   }
   ```
   - Control Panel sẽ tự động cập nhật!

### Scenario 3: WebSocket Real-time

1. **Mở Control Panel** trong browser

2. **Mở DevTools** (F12) → Tab Console

3. **Gửi telemetry liên tục** từ MQTTX (mỗi 3 giây):
   ```json
   {
     "sensors": {
       "temperature": 28.5,
       "airHumidity": 65.0,
       "light": 450.0,
       "soilMoisture": 45.0
     }
   }
   ```
   (Thay đổi giá trị mỗi lần)

4. **Quan sát UI tự động cập nhật**:
   - Số liệu thay đổi
   - Progress bar di chuyển
   - Không cần refresh trang!

---

## 🔧 Cấu trúc Project

```
Template/
├── src/main/resources/static/     # Frontend files
│   ├── home.html                  # Landing page
│   ├── dashboard.html             # Dashboard với biểu đồ
│   ├── control.html               # Control Panel real-time
│   └── index.html                 # AI Chat
│
├── src/main/java/com/example/demo/
│   ├── controller/
│   │   ├── DeviceController.java        # REST API
│   │   └── AiChatController.java        # AI Chat API
│   ├── service/
│   │   ├── MqttProcessingService.java   # MQTT Inbound
│   │   ├── CommandService.java          # MQTT Outbound
│   │   ├── DeviceStateService.java      # Redis Cache
│   │   ├── TelemetryService.java        # MySQL History
│   │   └── NotificationService.java     # WebSocket
│   └── configuration/
│       ├── MqttConfig.java              # MQTT Config
│       └── WebSocketConfig.java         # WebSocket Config
│
├── ESP32_SmartGarden/
│   ├── ESP32_SmartGarden.ino      # Arduino code
│   └── README.md                  # Hướng dẫn ESP32
│
└── docker-compose.yml             # Docker orchestration
```

---

## 📊 Kiến trúc hệ thống

```
┌─────────────┐
│   ESP32     │ ── MQTT ──┐
│  (Sensors)  │           │
└─────────────┘           │
                          ↓
┌─────────────┐     ┌──────────────┐
│   MQTTX     │ ──→ │ MQTT Broker  │
│  (Test)     │     │  (Mosquitto) │
└─────────────┘     └──────┬───────┘
                           │
                           ↓
                  ┌────────────────┐
                  │ Spring Boot    │
                  │ Backend        │
                  └─────┬──────────┘
                        │
        ┌───────────────┼───────────────┐
        ↓               ↓               ↓
   ┌────────┐      ┌────────┐    ┌──────────┐
   │ MySQL  │      │ Redis  │    │WebSocket │
   │(History)      │(Cache) │    │(Real-time│
   └────────┘      └────────┘    └─────┬────┘
                                        │
                                        ↓
                                 ┌──────────────┐
                                 │   Frontend   │
                                 │ (HTML/CSS/JS)│
                                 └──────────────┘
```

---

## 🎨 Screenshots mô tả

### Dashboard:
- Header: Device selector + Time range buttons (1 Day / 1 Week)
- Stats Cards: 4 cards hiển thị giá trị trung bình
- Charts: 4 biểu đồ line chart (nhiệt độ, độ ẩm KK, ánh sáng, độ ẩm đất)

### Control Panel:
- Left Panel: Control buttons (Pump toggle, MANUAL/AUTO, Quick stats)
- Right Grid: 4 cards hiển thị sensor values real-time với progress bars

### AI Chat:
- Chat interface với tin nhắn
- Input box với nút Send
- Nút Microphone (Speech-to-Text)

---

## 📚 Tài liệu tham khảo

- **Frontend Guide**: [FRONTEND_GUIDE.md](./FRONTEND_GUIDE.md)
- **IntelliJ Guide**: [HUONG_DAN_CHAY_INTELLIJ.md](./HUONG_DAN_CHAY_INTELLIJ.md)
- **ESP32 Code**: [ESP32_SmartGarden/README.md](./ESP32_SmartGarden/README.md)
- **Architecture**: [ESP32_SmartGarden/ARCHITECTURE.md](./ESP32_SmartGarden/ARCHITECTURE.md)
- **API Docs**: http://localhost:8080/swagger-ui.html

---

## 💡 Tips

### Tip 1: Test nhanh Dashboard
```powershell
# Script gửi nhiều telemetry để test biểu đồ
# (Dùng MQTTX hoặc tạo script Python)
```

### Tip 2: Debug WebSocket
```javascript
// Mở Console trong Control Panel, chạy:
console.log('WebSocket connected:', stompClient.connected);
```

### Tip 3: Xem dữ liệu trong Redis
```powershell
docker exec -it redis_cache redis-cli
KEYS *
GET device:state:ESP32_GARDEN_001
```

### Tip 4: Xem dữ liệu trong MySQL
```powershell
docker exec -it mysql_db mysql -uroot -proot iot_db

# Trong MySQL:
SELECT * FROM device_entity;
SELECT * FROM telemetry_log ORDER BY created_at DESC LIMIT 10;
```

---

## 🐛 Common Issues

### Issue 1: Dashboard không có dữ liệu
**Giải pháp**: Gửi ít nhất 5-10 telemetry messages, đợi vài giây, refresh page.

### Issue 2: Control Panel không cập nhật
**Giải pháp**: 
- Kiểm tra Console (F12) có lỗi WebSocket không
- Kiểm tra backend logs: `docker-compose logs -f app`

### Issue 3: Không điều khiển được máy bơm
**Giải pháp**: 
- Đảm bảo device status = ONLINE
- Chuyển sang chế độ MANUAL

---

## 🎓 Học thêm

### Muốn thêm authentication?
- Tích hợp Spring Security
- Thêm JWT tokens
- Login/Register pages

### Muốn thêm notification?
- Thêm toast notification (Toastify)
- Email alerts (JavaMailSender)
- SMS alerts (Twilio)

### Muốn deploy production?
- Docker Compose production mode
- Nginx reverse proxy
- HTTPS với Let's Encrypt
- CI/CD với GitHub Actions

---

## 🎉 Kết luận

Bạn đã có một hệ thống Smart Garden IoT hoàn chỉnh với:

✅ Backend Spring Boot với MQTT, WebSocket, Redis, MySQL
✅ Frontend đẹp mắt với Dashboard và Control Panel
✅ Real-time updates qua WebSocket
✅ Biểu đồ thống kê với Chart.js
✅ AI Chat với Gemini
✅ ESP32 code hoàn chỉnh
✅ Docker containerization

**Chúc bạn demo thành công! 🌱🚀**

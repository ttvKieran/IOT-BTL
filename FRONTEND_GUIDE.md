# 🌱 Smart Garden - Frontend Guide

## 📄 Các trang có sẵn

### 1. **Trang chủ** (`/home.html`)
- Giới thiệu tổng quan về hệ thống
- Hiển thị các tính năng nổi bật
- Trạng thái hệ thống (số thiết bị, thiết bị online)
- Link nhanh đến các chức năng

**URL**: http://localhost:8080/home.html

---

### 2. **Dashboard** (`/dashboard.html`)
📊 Xem biểu đồ và thống kê dữ liệu từ database

**Tính năng:**
- ✅ Chọn thiết bị từ danh sách
- ✅ Xem dữ liệu theo **1 ngày** hoặc **1 tuần**
- ✅ 4 thống kê nhanh: Nhiệt độ TB, Độ ẩm TB, Ánh sáng TB, Độ ẩm đất TB
- ✅ 4 biểu đồ line chart (Chart.js):
  - Nhiệt độ (°C)
  - Độ ẩm không khí (%)
  - Ánh sáng (lux)
  - Độ ẩm đất (%)

**API sử dụng:**
- `GET /api/v1/devices` - Lấy danh sách thiết bị
- `GET /api/v1/devices/{deviceUid}/history?from={from}&to={to}` - Lấy lịch sử từ MySQL

**URL**: http://localhost:8080/dashboard.html

---

### 3. **Control Panel** (`/control.html`)
🎛️ Điều khiển thiết bị và xem dữ liệu real-time

**Tính năng:**
- ✅ Hiển thị trạng thái thiết bị (Online/Offline)
- ✅ Điều khiển máy bơm (ON/OFF) bằng toggle switch
- ✅ Chuyển chế độ: MANUAL / AUTO
- ✅ Hiển thị dữ liệu cảm biến **real-time** qua WebSocket:
  - Nhiệt độ (°C)
  - Độ ẩm không khí (%)
  - Ánh sáng (lux)
  - Độ ẩm đất (%)
- ✅ Progress bar cho mỗi cảm biến
- ✅ Thống kê nhanh bên cạnh panel điều khiển

**API sử dụng:**
- `GET /api/v1/devices/{deviceUid}/state` - Lấy trạng thái hiện tại từ Redis
- `POST /api/v1/devices/{deviceUid}/command` - Gửi lệnh điều khiển
- `WebSocket /ws` - Nhận cập nhật real-time

**URL**: http://localhost:8080/control.html

---

### 4. **AI Chat** (`/index.html`)
🤖 Trò chuyện với trợ lý AI Synthia

**Tính năng:**
- ✅ Chat với AI để hỏi về trạng thái vườn
- ✅ Điều khiển thiết bị bằng giọng nói (Speech-to-Text)
- ✅ Gửi lệnh bằng văn bản
- ✅ AI tích hợp Gemini

**API sử dụng:**
- `POST /api/v1/ai/chat/{deviceUid}` - Gửi tin nhắn cho AI

**URL**: http://localhost:8080/index.html

---

## 🚀 Cách sử dụng

### Bước 1: Khởi động Backend

**Option A: Docker** (Khuyến nghị)
```powershell
docker-compose up -d
```

**Option B: Maven**
```powershell
.\mvnw spring-boot:run
```

### Bước 2: Truy cập Frontend

Mở trình duyệt và truy cập:

| Trang | URL | Mô tả |
|-------|-----|-------|
| **Trang chủ** | http://localhost:8080/home.html | Landing page |
| **Dashboard** | http://localhost:8080/dashboard.html | Biểu đồ thống kê |
| **Control Panel** | http://localhost:8080/control.html | Điều khiển real-time |
| **AI Chat** | http://localhost:8080/index.html | Trợ lý AI |
| **Swagger** | http://localhost:8080/swagger-ui.html | API Documentation |

### Bước 3: Đăng ký thiết bị (nếu chưa có)

**Cách 1: Qua Swagger UI**
1. Mở http://localhost:8080/swagger-ui.html
2. Tìm **Device Controller** → **POST /api/v1/devices**
3. Click **Try it out**
4. Nhập:
```json
{
  "deviceUid": "ESP32_GARDEN_001",
  "name": "Vườn tầng thượng"
}
```
5. Click **Execute**

**Cách 2: Qua Postman**
```
POST http://localhost:8080/api/v1/devices
Content-Type: application/json

{
  "deviceUid": "ESP32_GARDEN_001",
  "name": "Vườn tầng thượng"
}
```

### Bước 4: Upload code lên ESP32

Xem hướng dẫn chi tiết trong: `ESP32_SmartGarden/README.md`

---

## 📡 Luồng dữ liệu

### 1. Dashboard (Dữ liệu lịch sử)
```
User chọn device và khoảng thời gian
  ↓
Frontend gọi API GET /devices/{deviceUid}/history
  ↓
Backend query MySQL (TelemetryLog table)
  ↓
Trả về array of TelemetryLogDto
  ↓
Frontend vẽ biểu đồ bằng Chart.js
```

### 2. Control Panel (Real-time)
```
Frontend kết nối WebSocket /ws
  ↓
Subscribe topic: /topic/devices/{deviceUid}
  ↓
ESP32 gửi dữ liệu qua MQTT
  ↓
Backend nhận MQTT → Update Redis → Broadcast WebSocket
  ↓
Frontend nhận message → Update UI real-time
```

### 3. Điều khiển máy bơm
```
User click toggle switch
  ↓
Frontend gọi POST /devices/{deviceUid}/command
Body: { "action": "CONTROL_PUMP", "payload": { "state": "ON" } }
  ↓
Backend gửi lệnh qua MQTT Outbound
  ↓
ESP32 nhận lệnh → Bật relay máy bơm → Gửi state update
  ↓
Frontend nhận update qua WebSocket → UI tự động cập nhật
```

---

## 🎨 Stack công nghệ Frontend

- **UI Framework**: Tailwind CSS (CDN)
- **Charts**: Chart.js
- **WebSocket**: SockJS + STOMP.js
- **Icons**: Heroicons (SVG)
- **Responsive**: Mobile-first design

---

## 🐛 Troubleshooting

### ❌ Dashboard không hiển thị dữ liệu

**Nguyên nhân:** Chưa có dữ liệu trong database

**Giải pháp:**
1. ESP32 phải chạy và gửi telemetry
2. Hoặc dùng MQTTX để giả lập:
```json
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
3. Đợi vài giây để backend lưu vào MySQL
4. Refresh dashboard

---

### ❌ Control Panel không cập nhật real-time

**Nguyên nhân:** WebSocket không kết nối

**Giải pháp:**
1. Mở Console (F12) → Tab Console
2. Kiểm tra có lỗi WebSocket không
3. Đảm bảo backend đang chạy
4. Kiểm tra WebSocket config trong backend:
```java
// WebSocketConfig.java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
}
```

---

### ❌ Không điều khiển được máy bơm

**Nguyên nhân 1:** Thiết bị Offline

**Giải pháp:** 
- Kiểm tra ESP32 đã kết nối WiFi chưa
- Kiểm tra ESP32 đã kết nối MQTT chưa

**Nguyên nhân 2:** Đang ở chế độ AUTO

**Giải pháp:**
- Chuyển sang chế độ MANUAL
- Mới có thể điều khiển máy bơm thủ công

---

### ❌ Biểu đồ không hiển thị

**Nguyên nhân:** Chart.js không load

**Giải pháp:**
1. Kiểm tra kết nối internet (Chart.js load từ CDN)
2. Hoặc download Chart.js về local:
```html
<script src="/js/chart.min.js"></script>
```

---

## 📱 Responsive Design

Frontend đã được thiết kế responsive:

- ✅ **Desktop** (>1024px): Hiển thị đầy đủ 3-4 cột
- ✅ **Tablet** (768px-1024px): 2 cột
- ✅ **Mobile** (<768px): 1 cột, stack vertical

---

## 🔐 Security Notes

**Lưu ý:**
- Frontend hiện tại **KHÔNG có authentication**
- Phù hợp cho demo hoặc mạng nội bộ
- Nếu deploy production, cần thêm:
  - Spring Security
  - JWT Token
  - HTTPS
  - CORS configuration

---

## 🚧 Future Improvements

- [ ] Thêm authentication (Login/Register)
- [ ] Thêm notification khi thiết bị offline
- [ ] Thêm export data (CSV, Excel)
- [ ] Thêm dark mode
- [ ] Thêm mobile app (React Native)
- [ ] Thêm email/SMS alerts
- [ ] Thêm camera streaming
- [ ] Multi-language support

---

## 📞 Hỗ trợ

Nếu gặp vấn đề:
1. Kiểm tra console browser (F12)
2. Kiểm tra logs backend
3. Xem API docs: http://localhost:8080/swagger-ui.html
4. Đọc file `HUONG_DAN_CHAY_INTELLIJ.md`

---

**Chúc bạn sử dụng vui vẻ! 🌱**

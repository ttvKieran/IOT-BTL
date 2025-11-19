# Hướng dẫn nạp code ESP32 - Quick Start

## 📋 Checklist trước khi nạp

- ✅ Backend đã chạy (Docker hoặc local)
- ✅ MQTT Broker đã chạy
- ✅ Arduino IDE đã cài đặt
- ✅ Thư viện đã cài đặt (xem bên dưới)
- ✅ ESP32 đã kết nối USB

---

## 🔧 Bước 1: Tìm IP máy chạy Backend/MQTT

### Windows:
```powershell
ipconfig
```
Tìm dòng **IPv4 Address** của adapter mạng đang dùng (WiFi hoặc Ethernet)

Ví dụ: 192.168.3.171

### Hoặc test ping từ máy khác:
```powershell
ping <IP_may_tinh>
```

---

## 🔧 Bước 2: Cấu hình code ESP32

Mở file `ESP32_SmartGarden.ino` và chỉnh sửa:

### 1. WiFi
```cpp
const char* WIFI_SSID = "Ten_WiFi_Cua_Ban";           // ← Thay đổi
const char* WIFI_PASSWORD = "Mat_Khau_WiFi";          // ← Thay đổi
```

### 2. MQTT Broker
```cpp
const char* MQTT_BROKER = "192.168.1.100";  // ← Thay bằng IP máy của bạn
const int MQTT_PORT = 1883;                  // ← Giữ nguyên (Docker expose 1883)
const char* MQTT_USERNAME = "iot_admin";     // ← Giữ nguyên (xem pwfile)
const char* MQTT_PASSWORD = "123456";        // ← Giữ nguyên (xem pwfile)
```

### 3. Device UID
```cpp
const char* DEVICE_UID = "ESP32_GARDEN_001"; // ← Có thể đổi (phải unique)
```

**⚠️ Lưu ý:** Device UID này phải đăng ký với backend qua API (xem Bước 5)

---

## 📚 Bước 3: Cài đặt thư viện Arduino

Mở Arduino IDE → **Tools** → **Manage Libraries** (Ctrl+Shift+I)

Cài đặt các thư viện sau:

| Thư viện | Tác giả | Version |
|----------|---------|---------|
| **PubSubClient** | Nick O'Leary | 2.8+ |
| **DHT sensor library** | Adafruit | 1.4.4+ |
| **Adafruit Unified Sensor** | Adafruit | 1.1.9+ |
| **BH1750** | Christopher Laws | 1.3.0+ |
| **ArduinoJson** | Benoit Blanchon | 6.21+ |

### Cài đặt ESP32 Board:

1. **File** → **Preferences**
2. **Additional Board Manager URLs**, thêm:
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
3. **Tools** → **Board** → **Boards Manager**
4. Tìm **esp32** by Espressif Systems → Install

---

## 🔌 Bước 4: Kết nối phần cứng

### Sơ đồ kết nối nhanh:

```
DHT11 (Data)          → GPIO 4
BH1750 (SDA)          → GPIO 21
BH1750 (SCL)          → GPIO 22
Soil Sensor (AO)      → GPIO 34
Relay Pump (IN)       → GPIO 5
```

### Chi tiết xem file: `README.md` trong thư mục này

---

## 📤 Bước 5: Upload code

1. **Tools** → **Board** → **ESP32 Dev Module**
2. **Tools** → **Port** → Chọn COM port của ESP32
3. **Sketch** → **Upload** (Ctrl+U)
4. Mở **Serial Monitor** (Ctrl+Shift+M) → Chọn **115200 baud**

### Kết quả mong đợi:

```
=== ESP32 Smart Garden Starting ===
BH1750 initialized successfully
Connecting to WiFi: Ten_WiFi_Cua_Ban
..........
WiFi connected!
IP Address: 192.168.1.150
Attempting MQTT connection...connected
Subscribed to: smartgarden/device/ESP32_GARDEN_001/command
✓ Status sent: ONLINE
✓ State sent: {...}
=== Sensor Data ===
Temperature: 28.5 °C
Air Humidity: 65.2 %
Light: 450.5 lx
Soil Moisture: 45.0 %
✓ Telemetry sent: {...}
```

---

## 🔗 Bước 6: Đăng ký Device với Backend

ESP32 đã gửi dữ liệu, nhưng backend chưa biết device này.

### Option A: Dùng Swagger UI (Dễ nhất)

1. Mở: http://localhost:8080/swagger-ui.html
2. Tìm **Device Controller** → **POST /api/devices**
3. Click **Try it out**
4. Nhập:
   ```json
   {
     "deviceUid": "ESP32_GARDEN_001",
     "name": "Vườn nhà tôi"
   }
   ```
5. Click **Execute**
6. Xem Response: `200 OK` → Thành công!

### Option B: Dùng Postman

```
POST http://localhost:8080/api/devices
Content-Type: application/json

{
  "deviceUid": "ESP32_GARDEN_001",
  "name": "Vườn nhà tôi"
}
```

### Option C: Dùng cURL

```powershell
curl -X POST http://localhost:8080/api/devices `
  -H "Content-Type: application/json" `
  -d '{\"deviceUid\":\"ESP32_GARDEN_001\",\"name\":\"Vườn nhà tôi\"}'
```

---

## ✅ Bước 7: Kiểm tra hoạt động

### 1. Kiểm tra Dashboard

Mở: http://localhost:8080/home.html

Bạn sẽ thấy:
- 📊 Dashboard: Biểu đồ dữ liệu cảm biến
- 🎮 Control Panel: Dữ liệu real-time và điều khiển máy bơm

### 2. Test điều khiển máy bơm

#### Dùng Control Panel:
1. Vào: http://localhost:8080/control.html
2. Click nút **Turn ON** hoặc **Turn OFF**
3. Xem Serial Monitor của ESP32 → Sẽ thấy:
   ```
   Message received on topic: smartgarden/device/ESP32_GARDEN_001/command
   Payload: {"action":"CONTROL_PUMP","payload":{"state":"ON"}}
   Command: Turn pump ON
   Pump ON
   ✓ State sent: {...}
   ```
4. Relay sẽ click (máy bơm bật)

#### Dùng API:
```
POST http://localhost:8080/api/devices/ESP32_GARDEN_001/command
Content-Type: application/json

{
  "action": "CONTROL_PUMP",
  "payload": {
    "state": "ON"
  }
}
```

### 3. Kiểm tra MQTT trực tiếp

Dùng MQTTX:
1. Connect đến: `localhost:1883`
2. Username: `iot_admin`, Password: `123456`
3. Subscribe: `smartgarden/device/ESP32_GARDEN_001/#`
4. Sẽ thấy các message:
   - `/telemetry` - Dữ liệu cảm biến (mỗi 10s)
   - `/status` - Trạng thái ONLINE (mỗi 5s)
   - `/state` - Trạng thái đầy đủ

---

## 🐛 Troubleshooting

### ❌ ESP32 không kết nối WiFi

**Nguyên nhân:** SSID/Password sai hoặc WiFi 5GHz

**Giải pháp:**
- Kiểm tra lại SSID và Password
- ESP32 chỉ hỗ trợ WiFi 2.4GHz (không dùng 5GHz)
- Kiểm tra cường độ sín hiệu WiFi

### ❌ MQTT connection failed, rc=-2

**Nguyên nhân:** Không kết nối được MQTT Broker

**Giải pháp:**
```powershell
# Kiểm tra MQTT Broker đã chạy
docker ps | findstr mqtt

# Test ping
ping <IP_MQTT_BROKER>

# Test port
telnet <IP_MQTT_BROKER> 1883
```

### ❌ MQTT connection failed, rc=4

**Nguyên nhân:** Username/Password sai

**Giải pháp:**
- Kiểm tra file `mqtt_broker/mosquitto/config/pwfile`
- Username phải là: `iot_admin`
- Password phải là: `123456`

### ❌ MQTT connection failed, rc=5

**Nguyên nhân:** Không có quyền truy cập

**Giải pháp:**
- Kiểm tra file `mosquitto.conf`:
  ```
  allow_anonymous false
  password_file /mosquitto/config/pwfile
  listener 1883 0.0.0.0
  ```

### ❌ DHT11 trả về NaN

**Nguyên nhân:** Cảm biến chưa sẵn sàng hoặc kết nối sai

**Giải pháp:**
- Kiểm tra kết nối dây
- Thêm điện trở pull-up 10kΩ giữa Data và VCC
- DHT11 cần ~2 giây để khởi động

### ❌ BH1750 không đọc được

**Nguyên nhân:** I2C không hoạt động

**Giải pháp:**
```cpp
// Thêm vào setup() để scan I2C
Wire.begin(21, 22);
Wire.beginTransmission(0x23);
if (Wire.endTransmission() == 0) {
  Serial.println("BH1750 found at 0x23");
} else {
  Serial.println("BH1750 not found!");
}
```

### ❌ Backend không nhận được dữ liệu

**Nguyên nhân:** Format JSON không đúng hoặc device chưa đăng ký

**Giải pháp:**
1. Kiểm tra Serial Monitor → Xem message gửi đi
2. Kiểm tra backend logs:
   ```powershell
   docker logs -f smart_garden_app
   ```
3. Đảm bảo device đã đăng ký (Bước 6)

---

## 📊 Format JSON chuẩn

### ESP32 → Backend (Telemetry)
```json
{
  "sensors": {
    "temperature": 28.5,
    "air_humidity": 65.2,
    "light": 450.5,
    "soil_moisture": 45.0
  },
  "timestamp": 123456
}
```

### ESP32 → Backend (State)
```json
{
  "device_uid": "ESP32_GARDEN_001",
  "status": "ONLINE",
  "control_mode": "MANUAL",
  "pump_state": "OFF",
  "sensors": {
    "temperature": 28.5,
    "air_humidity": 65.2,
    "light": 450.5,
    "soil_moisture": 45.0
  },
  "timestamp": 123456,
  "last_seen": 123456
}
```

### Backend → ESP32 (Command)
```json
{
  "action": "CONTROL_PUMP",
  "payload": {
    "state": "ON"
  }
}
```

**⚠️ Lưu ý:** Backend dùng **snake_case** (air_humidity, soil_moisture) theo cấu hình Jackson

---

## 🎯 Tóm tắt các bước

1. ✅ Tìm IP máy chạy Backend/MQTT
2. ✅ Cấu hình WiFi và MQTT trong code
3. ✅ Cài đặt thư viện Arduino
4. ✅ Kết nối phần cứng
5. ✅ Upload code lên ESP32
6. ✅ Đăng ký device qua API
7. ✅ Test trên Dashboard và Control Panel

---

## 📚 Tài liệu tham khảo

- **README chi tiết**: [README.md](./README.md)
- **Sơ đồ kiến trúc**: [ARCHITECTURE.md](./ARCHITECTURE.md)
- **Hướng dẫn backend**: [../HUONG_DAN_CHAY_INTELLIJ.md](../HUONG_DAN_CHAY_INTELLIJ.md)
- **Dashboard**: http://localhost:8080/home.html
- **Control Panel**: http://localhost:8080/control.html
- **Swagger API**: http://localhost:8080/swagger-ui.html

---

Chúc bạn nạp code thành công! 🎉

Nếu gặp lỗi, tham khảo phần **Troubleshooting** hoặc xem logs chi tiết.

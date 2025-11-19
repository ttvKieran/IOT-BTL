# ESP32 Smart Garden - Hướng dẫn cài đặt

## 📋 Mục lục
- [Thư viện cần cài đặt](#thư-viện-cần-cài-đặt)
- [Sơ đồ kết nối phần cứng](#sơ-đồ-kết-nối-phần-cứng)
- [Cấu hình code](#cấu-hình-code)
- [Cấu trúc MQTT Topics](#cấu-trúc-mqtt-topics)
- [Cấu trúc JSON Messages](#cấu-trúc-json-messages)
- [Troubleshooting](#troubleshooting)

## 📚 Thư viện cần cài đặt

Mở Arduino IDE và cài đặt các thư viện sau qua Library Manager (`Sketch` → `Include Library` → `Manage Libraries`):

1. **PubSubClient** by Nick O'Leary (v2.8.0 trở lên)
   - Thư viện MQTT client cho Arduino
   
2. **DHT sensor library** by Adafruit (v1.4.4 trở lên)
   - Thư viện đọc cảm biến DHT11/DHT22
   - Yêu cầu: **Adafruit Unified Sensor** (cài đặt kèm)
   
3. **BH1750** by Christopher Laws (v1.3.0 trở lên)
   - Thư viện đọc cảm biến ánh sáng BH1750
   
4. **ArduinoJson** by Benoit Blanchon (v6.21.0 trở lên)
   - Thư viện xử lý JSON

### Cài đặt Board ESP32

1. Mở `File` → `Preferences`
2. Thêm URL sau vào `Additional Board Manager URLs`:
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
3. Mở `Tools` → `Board` → `Boards Manager`
4. Tìm và cài đặt **esp32** by Espressif Systems

## 🔌 Sơ đồ kết nối phần cứng

### ESP32 Pinout

| Cảm biến/Thiết bị | Chân ESP32 | Ghi chú |
|-------------------|------------|---------|
| **DHT11 (Data)** | GPIO 4 | Cần điện trở pull-up 10kΩ |
| **BH1750 (SDA)** | GPIO 21 | I2C Data |
| **BH1750 (SCL)** | GPIO 22 | I2C Clock |
| **Soil Moisture (AO)** | GPIO 34 (ADC1_CH6) | Analog Input |
| **Relay Máy bơm** | GPIO 5 | Digital Output |

### Chi tiết kết nối

#### 1. DHT11 (Cảm biến nhiệt độ và độ ẩm không khí)
```
DHT11          ESP32
-----          -----
VCC    ----→   3.3V
GND    ----→   GND
DATA   ----→   GPIO 4 (qua điện trở 10kΩ lên 3.3V)
```

#### 2. BH1750 (Cảm biến ánh sáng)
```
BH1750         ESP32
------         -----
VCC    ----→   3.3V
GND    ----→   GND
SDA    ----→   GPIO 21
SCL    ----→   GPIO 22
ADDR   ----→   GND (địa chỉ I2C: 0x23)
```

#### 3. Cảm biến độ ẩm đất (Capacitive/Resistive)
```
Soil Sensor    ESP32
-----------    -----
VCC    ----→   3.3V hoặc 5V (tùy loại cảm biến)
GND    ----→   GND
AO     ----→   GPIO 34 (Analog Output)
```

#### 4. Module Relay (Điều khiển máy bơm)
```
Relay          ESP32          Máy bơm
-----          -----          -------
VCC    ----→   5V
GND    ----→   GND
IN     ----→   GPIO 5
COM    ----→   Nguồn máy bơm (+)
NO     ----→   Máy bơm (+)
                              Máy bơm (-) → Nguồn (-)
```

**⚠️ Lưu ý quan trọng:**
- Máy bơm nên dùng nguồn riêng (5V-12V tùy loại), không dùng chung với ESP32
- Relay thường dùng 5V, có thể cần module chuyển đổi mức logic 3.3V→5V
- GPIO 34-39 chỉ làm INPUT, không có pull-up nội

## ⚙️ Cấu hình code

Mở file `ESP32_SmartGarden.ino` và chỉnh sửa các thông số sau:

### 1. WiFi
```cpp
const char* WIFI_SSID = "YourWiFiSSID";         // Tên WiFi
const char* WIFI_PASSWORD = "YourWiFiPassword"; // Mật khẩu WiFi
```

### 2. MQTT Broker
```cpp
const char* MQTT_BROKER = "192.168.1.100";  // IP của MQTT Broker
const int MQTT_PORT = 18883;                 // Port (mặc định: 1883, docker: 18883)
const char* MQTT_USERNAME = "iot_admin";    // Username MQTT
const char* MQTT_PASSWORD = "123456";       // Password MQTT
const char* DEVICE_UID = "ESP32_GARDEN_001"; // ID thiết bị (phải unique)
```

### 3. Chân GPIO (nếu khác sơ đồ trên)
```cpp
#define DHT_PIN 4              // DHT11 data pin
#define SOIL_MOISTURE_PIN 34   // Soil moisture analog pin
#define PUMP_PIN 5             // Relay control pin
#define I2C_SDA 21             // I2C SDA
#define I2C_SCL 22             // I2C SCL
```

### 4. Khoảng thời gian gửi dữ liệu
```cpp
const unsigned long TELEMETRY_INTERVAL = 10000;  // Gửi dữ liệu cảm biến mỗi 10 giây
const unsigned long HEARTBEAT_INTERVAL = 5000;   // Gửi heartbeat mỗi 5 giây
```

## 📡 Cấu trúc MQTT Topics

### Topics ESP32 publish (gửi lên server):

1. **Telemetry** (Dữ liệu cảm biến)
   ```
   smartgarden/device/{deviceUid}/telemetry
   ```

2. **Status** (Trạng thái kết nối)
   ```
   smartgarden/device/{deviceUid}/status
   ```

3. **State** (Trạng thái đầy đủ thiết bị)
   ```
   smartgarden/device/{deviceUid}/state
   ```

### Topics ESP32 subscribe (nhận từ server):

1. **Command** (Lệnh điều khiển)
   ```
   smartgarden/device/{deviceUid}/command
   ```

## 📦 Cấu trúc JSON Messages

### 1. Telemetry (ESP32 → Server)
```json
{
  "sensors": {
    "temperature": 28.5,
    "airHumidity": 65.2,
    "light": 450.5,
    "soilMoisture": 45.0
  },
  "timestamp": 123456789
}
```

### 2. Status (ESP32 → Server)
```json
{
  "status": "ONLINE",
  "timestamp": 123456789
}
```

### 3. State (ESP32 → Server)
```json
{
  "deviceUid": "ESP32_GARDEN_001",
  "status": "ONLINE",
  "controlMode": "MANUAL",
  "pumpState": "OFF",
  "sensors": {
    "temperature": 28.5,
    "airHumidity": 65.2,
    "light": 450.5,
    "soilMoisture": 45.0
  },
  "timestamp": 123456789
}
```

### 4. Command - Control Pump (Server → ESP32)
```json
{
  "action": "CONTROL_PUMP",
  "payload": {
    "state": "ON"
  }
}
```
hoặc
```json
{
  "action": "CONTROL_PUMP",
  "payload": {
    "state": "OFF"
  }
}
```

### 5. Command - Set Mode (Server → ESP32)
```json
{
  "action": "SET_MODE",
  "payload": {
    "mode": "AUTO"
  }
}
```
hoặc
```json
{
  "action": "SET_MODE",
  "payload": {
    "mode": "MANUAL"
  }
}
```

### 6. Command - Request State (Server → ESP32)
```json
{
  "action": "REQUEST_STATE"
}
```

## 🚀 Upload và chạy

1. Kết nối ESP32 với máy tính qua USB
2. Chọn board: `Tools` → `Board` → `ESP32 Dev Module`
3. Chọn port: `Tools` → `Port` → chọn COM port tương ứng
4. Upload code: `Sketch` → `Upload` (Ctrl+U)
5. Mở Serial Monitor: `Tools` → `Serial Monitor` (115200 baud)

## 🔍 Troubleshooting

### WiFi không kết nối được
- Kiểm tra SSID và password
- Đảm bảo WiFi là 2.4GHz (ESP32 không hỗ trợ 5GHz)
- Kiểm tra cường độ sín hiệu WiFi

### MQTT không kết nối được
- Kiểm tra IP và port của MQTT Broker
- Kiểm tra username/password
- Dùng MQTT Explorer hoặc MQTT.fx để test broker
- Kiểm tra firewall

### Cảm biến DHT11 trả về NaN
- Kiểm tra kết nối dây
- Kiểm tra điện trở pull-up 10kΩ
- Thử đổi chân GPIO khác
- DHT11 cần thời gian khởi động ~2 giây

### BH1750 không đọc được
- Kiểm tra địa chỉ I2C bằng I2C Scanner
- Đảm bảo chân SDA/SCL kết nối đúng
- Kiểm tra nguồn 3.3V

### Cảm biến độ ẩm đất không chính xác
- Hiệu chỉnh lại công thức map() trong code
- Đo giá trị ADC khi khô hoàn toàn và ướt hoàn toàn
- Cập nhật giá trị min/max trong hàm map()

### Máy bơm không hoạt động
- Kiểm tra relay có click không
- Kiểm tra nguồn máy bơm riêng biệt
- Kiểm tra logic HIGH/LOW của relay (có loại active HIGH, có loại active LOW)
- Thử đổi `digitalWrite(PUMP_PIN, pumpState ? HIGH : LOW)` thành `LOW : HIGH`

## 📝 Ghi chú

- Code được tối ưu cho ESP32 DevKit v1
- Có thể mở rộng thêm cảm biến khác
- Có thể thêm chế độ AUTO để tự động bật máy bơm khi độ ẩm đất thấp
- Nên sử dụng watchdog timer để tự động reset khi ESP32 bị treo

## 📄 License

MIT License - Tự do sử dụng và chỉnh sửa

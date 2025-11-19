/**
 * File cấu hình mẫu - Sao chép sang ESP32_SmartGarden.ino và điền thông tin
 */

// ===================== CẤU HÌNH WiFi =====================
const char* WIFI_SSID = "YourWiFiSSID";           
const char* WIFI_PASSWORD = "YourWiFiPassword";   

// ===================== CẤU HÌNH MQTT =====================
// Nếu chạy MQTT Broker bằng Docker:
// - MQTT_BROKER: địa chỉ IP máy chạy Docker (ví dụ: 192.168.1.100)
// - MQTT_PORT: 18883 (như cấu hình docker-compose.yml)
//
// Nếu chạy MQTT Broker local không qua Docker:
// - MQTT_BROKER: localhost hoặc 127.0.0.1
// - MQTT_PORT: 1883

const char* MQTT_BROKER = "192.168.1.100";        // Thay bằng IP máy của bạn
const int MQTT_PORT = 18883;                       // Port như trong docker-compose.yml
const char* MQTT_USERNAME = "iot_admin";          // Như trong application.properties
const char* MQTT_PASSWORD = "123456";             // Như trong application.properties

// Device UID - Mỗi ESP32 cần một UID riêng biệt
// Ví dụ: ESP32_GARDEN_001, ESP32_GARDEN_002, ...
const char* DEVICE_UID = "ESP32_GARDEN_001";

// ===================== HƯỚNG DẪN TÌM IP MÁY CHẠY DOCKER =====================
// 
// Windows:
// 1. Mở CMD hoặc PowerShell
// 2. Gõ: ipconfig
// 3. Tìm "IPv4 Address" của adapter mạng đang dùng (WiFi hoặc Ethernet)
//    Ví dụ: 192.168.1.100
//
// Linux/Mac:
// 1. Mở Terminal
// 2. Gõ: ifconfig hoặc ip addr
// 3. Tìm địa chỉ inet của interface đang dùng (wlan0, eth0, ...)
//    Ví dụ: 192.168.1.100
//
// ===================== TEST MQTT BROKER =====================
//
// Sử dụng MQTT Explorer để test:
// 1. Download: http://mqtt-explorer.com/
// 2. Kết nối với:
//    - Host: IP máy của bạn (ví dụ: 192.168.1.100)
//    - Port: 18883
//    - Username: iot_admin
//    - Password: 123456
// 3. Nếu kết nối thành công → Cấu hình đúng
//
// ===================== KIỂM TRA KẾT NỐI =====================
//
// Ping MQTT Broker từ máy tính:
// Windows: ping 192.168.1.100
// Linux/Mac: ping 192.168.1.100
//
// Nếu không ping được → Kiểm tra:
// - Firewall
// - Docker có đang chạy không
// - Port mapping trong docker-compose.yml

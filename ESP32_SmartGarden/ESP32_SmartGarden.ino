/**
 * ESP32 Smart Garden - Code Arduino
 * Cảm biến: DHT11 (nhiệt độ, độ ẩm không khí), BH1750 (ánh sáng), Cảm biến độ ẩm đất
 * Actuator: Máy bơm nước
 * Giao thức: MQTT
 * Tương thích với Backend Spring Boot
 */

#include <WiFi.h>
#include <PubSubClient.h>
#include <DHT.h>
#include <Wire.h>
#include <BH1750.h>
#include <ArduinoJson.h>

// ===================== CẤU HÌNH WiFi =====================
const char* WIFI_SSID = "TOTOLINK_N350RT";           // Thay bằng tên WiFi của bạn
const char* WIFI_PASSWORD = "17081104";   // Thay bằng mật khẩu WiFi
// const char* ssid = "TOTOLINK_N350RT";
// const char* password = "17081104";
// ===================== CẤU HÌNH MQTT =====================
// Nếu chạy Docker trên máy khác:
//   - Thay 192.168.1.100 bằng IP máy chạy Docker
//   - Port: 1883 (Docker expose ra host)
// Nếu chạy local không Docker:
//   - MQTT_BROKER = "localhost" hoặc "127.0.0.1"
//   - Port: 1883
const char* MQTT_BROKER = "10.38.89.171";        // IP máy chạy Docker/Backend
const int MQTT_PORT = 1883;                       // Port (Docker expose 1883:1883)
const char* MQTT_USERNAME = "iot_admin";          // Username (xem pwfile)
const char* MQTT_PASSWORD = "123456";             // Password (xem pwfile)
const char* DEVICE_UID = "ESP32_GARDEN_001";      // ID thiết bị (phải đăng ký qua API)

// ===================== CẤU HÌNH CHÂN ESP32 =====================
#define DHT_PIN 4              // Chân kết nối cảm biến DHT11
#define DHT_TYPE DHT11         // Loại cảm biến DHT
#define SOIL_MOISTURE_PIN 34   // Chân ADC đọc cảm biến độ ẩm đất (Analog)
#define PUMP_PIN 5             // Chân điều khiển relay máy bơm
#define I2C_SDA 21             // Chân SDA cho I2C (BH1750)
#define I2C_SCL 22             // Chân SCL cho I2C (BH1750)

// ===================== CẤU HÌNH THỜI GIAN =====================
const unsigned long TELEMETRY_INTERVAL = 10000;  // Gửi dữ liệu telemetry mỗi 10 giây
const unsigned long HEARTBEAT_INTERVAL = 5000;   // Gửi heartbeat mỗi 5 giây

// ===================== MQTT TOPICS =====================
char TOPIC_TELEMETRY[100];    // smartgarden/device/{deviceUid}/telemetry
char TOPIC_STATUS[100];       // smartgarden/device/{deviceUid}/status
char TOPIC_COMMAND[100];      // smartgarden/device/{deviceUid}/command
char TOPIC_STATE[100];        // smartgarden/device/{deviceUid}/state

// ===================== KHỞI TẠO ĐỐI TƯỢNG =====================
WiFiClient espClient;
PubSubClient mqttClient(espClient);
DHT dht(DHT_PIN, DHT_TYPE);
BH1750 lightMeter;

// ===================== BIẾN TOÀN CỤC =====================
unsigned long lastTelemetryTime = 0;
unsigned long lastHeartbeatTime = 0;

// Trạng thái thiết bị
String controlMode = "MANUAL";  // "AUTO" hoặc "MANUAL"
bool pumpState = false;         // true = ON, false = OFF

// Dữ liệu cảm biến
float temperature = 0.0;
float airHumidity = 0.0;
float light = 0.0;
float soilMoisture = 0.0;

// ===================== SETUP =====================
void setup() {
  Serial.begin(115200);
  Serial.println("\n=== ESP32 Smart Garden Starting ===");
  
  // Khởi tạo chân I/O
  pinMode(PUMP_PIN, OUTPUT);
  digitalWrite(PUMP_PIN, HIGH);  // Tắt máy bơm ban đầu
  
  // Khởi tạo cảm biến
  dht.begin();
  Wire.begin(I2C_SDA, I2C_SCL);
  
  if (lightMeter.begin(BH1750::CONTINUOUS_HIGH_RES_MODE)) {
    Serial.println("BH1750 initialized successfully");
  } else {
    Serial.println("Error initializing BH1750");
  }
  
  // Kết nối WiFi
  connectWiFi();
  
  // Cấu hình MQTT
  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setKeepAlive(60);
  mqttClient.setSocketTimeout(30);
  
  // Tạo MQTT topics
  sprintf(TOPIC_TELEMETRY, "smartgarden/device/%s/telemetry", DEVICE_UID);
  sprintf(TOPIC_STATUS, "smartgarden/device/%s/status", DEVICE_UID);
  sprintf(TOPIC_COMMAND, "smartgarden/device/%s/command", DEVICE_UID);
  sprintf(TOPIC_STATE, "smartgarden/device/%s/state", DEVICE_UID);
  
  // Kết nối MQTT
  connectMQTT();
  
  Serial.println("=== Setup Complete ===");
}

// ===================== LOOP =====================
void loop() {
  // Đảm bảo kết nối WiFi và MQTT
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
  }
  
  if (!mqttClient.connected()) {
    connectMQTT();
  }
  
  mqttClient.loop();
  
  unsigned long currentTime = millis();
  
  // Gửi dữ liệu telemetry định kỳ
  if (currentTime - lastTelemetryTime >= TELEMETRY_INTERVAL) {
    lastTelemetryTime = currentTime;
    readSensors();
    sendTelemetry();
  }
  
  // Gửi heartbeat định kỳ
  if (currentTime - lastHeartbeatTime >= HEARTBEAT_INTERVAL) {
    lastHeartbeatTime = currentTime;
    sendHeartbeat();
  }
}

// ===================== KẾT NỐI WiFi =====================
void connectWiFi() {
  Serial.print("Connecting to WiFi: ");
  Serial.println(WIFI_SSID);
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi connected!");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\nFailed to connect to WiFi. Restarting...");
    delay(3000);
    ESP.restart();
  }
}

// ===================== KẾT NỐI MQTT =====================
void connectMQTT() {
  while (!mqttClient.connected()) {
    Serial.print("Connecting to MQTT Broker...");
    
    String clientId = "ESP32_" + String(DEVICE_UID);
    
    if (mqttClient.connect(clientId.c_str(), MQTT_USERNAME, MQTT_PASSWORD)) {
      Serial.println("Connected to MQTT!");
      
      // Subscribe vào topic command để nhận lệnh từ server
      mqttClient.subscribe(TOPIC_COMMAND);
      Serial.print("Subscribed to: ");
      Serial.println(TOPIC_COMMAND);
      
      // Gửi thông báo trạng thái online
      sendStatus("ONLINE");
      
    } else {
      Serial.print("Failed, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" - Retrying in 5 seconds...");
      delay(5000);
    }
  }
}

// ===================== CALLBACK MQTT =====================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received on topic: ");
  Serial.println(topic);
  
  // Chuyển payload thành chuỗi
  String message = "";
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  
  Serial.print("Payload: ");
  Serial.println(message);
  
  // Parse JSON
  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, message);
  
  if (error) {
    Serial.print("JSON parse error: ");
    Serial.println(error.c_str());
    return;
  }
  
  // Xử lý lệnh từ backend
  String action = doc["action"] | "";
  
  if (action == "CONTROL_PUMP") {
    // Backend format: {"action":"CONTROL_PUMP","payload":{"state":"ON"}}
    JsonObject payloadObj = doc["payload"];
    String state = payloadObj["state"] | "";
    
    if (state == "ON") {
      controlPump(true);
      Serial.println("Command: Turn pump ON");
    } else if (state == "OFF") {
      controlPump(false);
      Serial.println("Command: Turn pump OFF");
    }
    
  } else if (action == "SET_MODE") {
    // Backend format: {"action":"SET_MODE","payload":{"mode":"AUTO"}}
    JsonObject payloadObj = doc["payload"];
    String mode = payloadObj["mode"] | "";
    
    if (mode == "AUTO" || mode == "MANUAL") {
      controlMode = mode;
      Serial.print("Command: Control mode changed to: ");
      Serial.println(controlMode);
      sendState();
    }
    
  } else if (action == "REQUEST_STATE") {
    // Server yêu cầu trạng thái hiện tại
    Serial.println("Command: Request state");
    sendState();
  } else {
    Serial.print("Unknown action: ");
    Serial.println(action);
  }
}

// ===================== ĐỌC CẢM BIẾN =====================
void readSensors() {
  // Đọc DHT11
  temperature = dht.readTemperature();
  airHumidity = dht.readHumidity();
  
  // Kiểm tra lỗi DHT11
  if (isnan(temperature) || isnan(airHumidity)) {
    Serial.println("Failed to read from DHT sensor!");
    temperature = 0.0;
    airHumidity = 0.0;
  }
  
  // Đọc BH1750
  light = lightMeter.readLightLevel();
  if (light < 0) {
    Serial.println("Failed to read from BH1750 sensor!");
    light = 0.0;
  }
  
  // Đọc cảm biến độ ẩm đất (ADC)
  int soilValue = analogRead(SOIL_MOISTURE_PIN);
  // Chuyển đổi giá trị ADC (0-4095) sang phần trăm (0-100%)
  // Giả định: 4095 = khô (0%), 0 = ướt (100%)
  soilMoisture = map(soilValue, 4095, 0, 0, 100);
  soilMoisture = constrain(soilMoisture, 0, 100);
  
  // In ra Serial để debug
  Serial.println("=== Sensor Data ===");
  Serial.print("Temperature: ");
  Serial.print(temperature);
  Serial.println(" °C");
  Serial.print("Air Humidity: ");
  Serial.print(airHumidity);
  Serial.println(" %");
  Serial.print("Light: ");
  Serial.print(light);
  Serial.println(" lx");
  Serial.print("Soil Moisture: ");
  Serial.print(soilMoisture);
  Serial.println(" %");
}

// ===================== GỬI TELEMETRY =====================
void sendTelemetry() {
  // Format phù hợp với backend DeviceStateDTO.SensorData
  // Backend expect: snake_case (air_humidity, soil_moisture)
  StaticJsonDocument<512> doc;
  JsonObject sensors = doc.createNestedObject("sensors");
  
  sensors["temperature"] = temperature;
  sensors["air_humidity"] = airHumidity;      // Backend dùng snake_case
  sensors["light"] = light;
  sensors["soil_moisture"] = soilMoisture;    // Backend dùng snake_case
  
  doc["timestamp"] = millis();
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  if (mqttClient.publish(TOPIC_TELEMETRY, jsonString.c_str())) {
    Serial.print("✓ Telemetry sent: ");
    Serial.println(jsonString);
  } else {
    Serial.println("✗ Failed to send telemetry");
  }
}

// ===================== GỬI STATUS =====================
void sendStatus(const char* status) {
  // Backend expect: {"status": "ONLINE"} hoặc {"status": "OFFLINE"}
  StaticJsonDocument<256> doc;
  doc["status"] = status;  // Chữ hoa: ONLINE/OFFLINE
  doc["timestamp"] = millis();
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  mqttClient.publish(TOPIC_STATUS, jsonString.c_str(), true); // retained = true
  Serial.print("✓ Status sent: ");
  Serial.println(status);
}

// ===================== GỬI HEARTBEAT =====================
void sendHeartbeat() {
  sendStatus("ONLINE");
}

// ===================== GỬI STATE =====================
void sendState() {
  // Format phù hợp với backend DeviceStateDTO
  // Backend expect: snake_case cho tất cả fields
  StaticJsonDocument<512> doc;
  
  doc["device_uid"] = DEVICE_UID;           // snake_case
  doc["status"] = "ONLINE";
  doc["control_mode"] = controlMode;        // snake_case
  doc["pump_state"] = pumpState ? "ON" : "OFF";  // snake_case
  
  JsonObject sensors = doc.createNestedObject("sensors");
  sensors["temperature"] = temperature;
  sensors["air_humidity"] = airHumidity;    // snake_case
  sensors["light"] = light;
  sensors["soil_moisture"] = soilMoisture;  // snake_case
  
  doc["timestamp"] = millis();
  doc["last_seen"] = millis();              // Backend tracking
  
  String jsonString;
  serializeJson(doc, jsonString);
  
  if (mqttClient.publish(TOPIC_STATE, jsonString.c_str(), true)) {  // retained
    Serial.print("✓ State sent: ");
    Serial.println(jsonString);
  } else {
    Serial.println("✗ Failed to send state");
  }
}

// ===================== ĐIỀU KHIỂN MÁY BƠM =====================
void controlPump(bool turnOn) {
  pumpState = turnOn;
  digitalWrite(PUMP_PIN, pumpState ? LOW : HIGH);
  
  Serial.print("Pump ");
  Serial.println(pumpState ? "ON" : "OFF");
  
  // Gửi trạng thái mới lên server
  sendState();
}

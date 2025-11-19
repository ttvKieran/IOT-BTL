 # Smart Garden (Synthia) — Hướng dẫn dự án (Tiếng Việt)

Đây là repository demo cho hệ thống trợ lý vườn "Synthia" gồm backend, dịch vụ AI và frontend.

## 🚀 Quick Start

**Mới dùng IntelliJ IDEA?** → Xem hướng dẫn chi tiết: **[HUONG_DAN_CHAY_INTELLIJ.md](./HUONG_DAN_CHAY_INTELLIJ.md)**

**Code Arduino cho ESP32:** → Xem thư mục **[ESP32_SmartGarden/](./ESP32_SmartGarden/)**

Cấu trúc chính
- `src/` — Java Spring Boot backend (API, tích hợp MQTT, quản lý thiết bị)
- `AI/` — Python FastAPI (dịch vụ AI, trả về JSON khi cần gọi công cụ)
- `frontend/` — React + Vite (giao diện chat, hỗ trợ micro)
- `mqtt_broker/` — cấu hình Mosquitto và dữ liệu lưu trữ
- `docker-compose.yml` — chạy đồng thời backend, AI, MQTT, MySQL, Redis và frontend
- `ESP32_SmartGarden/` — Code Arduino cho ESP32 (DHT11, BH1750, Soil Sensor)

Mô tả ngắn
------------
Ứng dụng mô phỏng một backend IoT (Spring Boot) giao tiếp với các thiết bị qua MQTT và một trợ lý AI (FastAPI) để xử lý lệnh/ phản hồi. Frontend là một chat UI (React) cho phép người dùng trò chuyện với trợ lý, có thể ra lệnh điều khiển thiết bị (vd: bật bơm) thông qua một JSON "tool call" mà AI trả về.

Chạy nhanh bằng Docker
----------------------
Yêu cầu: Docker Desktop hoặc Docker & Docker Compose.

Từ thư mục gốc repo, chạy:

```cmd
docker-compose up --build -d
```

Kiểm tra trạng thái các container:

```cmd
docker-compose ps
```

Mở frontend trên trình duyệt:

http://localhost:3000

API backend (nếu cần):

http://localhost:8080

Ghi chú:
- Frontend được serve bằng Nginx trong container và ánh xạ ra cổng `3000`.
- Backend Spring Boot chạy trên cổng `8080` theo cấu hình `docker-compose.yml`.
- Dịch vụ AI chỉ cần truy cập nội bộ từ backend: `http://python-ai:8000`.

Chạy từng phần riêng (không dùng Docker)
------------------------------------

Frontend:

```cmd
cd frontend
npm install
npm run dev
# Mở URL do Vite in ra (thường http://localhost:5173)
```

Backend (Spring Boot):

```cmd
.\mvnw spring-boot:run
# hoặc build và chạy jar
.\mvnw -DskipTests package
java -jar target\*.jar
```

AI service (FastAPI):

```cmd
cd AI
python -m pip install -r requirements.txt
uvicorn AIService:app --host 0.0.0.0 --port 8000 --reload
```

Cấu hình quan trọng
-------------------
- MQTT (Mosquitto): cấu hình nằm ở `mqtt_broker/mosquitto/config/mosquitto.conf`. Nếu chạy broker cục bộ, đảm bảo đường dẫn `pwfile` và quyền truy cập chính xác.
- Spring Boot: cấu hình mặc định trong `src/main/resources/application.properties`. Các biến môi trường trong `docker-compose.yml` sẽ ghi đè khi chạy bằng Docker.

Chi tiết frontend
-----------------
- Ứng dụng React dùng Vite + Tailwind.
- Microphone (Speech-to-Text) sử dụng Web Speech API: chỉ hoạt động trên trình duyệt hỗ trợ và với HTTPS ở môi trường production (localhost ok cho dev).
- Hook `useSpeechRecognition` nằm trong `frontend/src/hooks/` và các component trong `frontend/src/components/`.

Khắc phục lỗi thường gặp
------------------------
- Lỗi build frontend (Vite/Rollup):
    - Kiểm tra `frontend/index.html` đảm bảo import `./src/main.jsx` bằng đường dẫn tương đối.
    - Chạy `npm run build` ở thư mục `frontend` để xem lỗi chi tiết.
    - Docker có thể cache layer cũ; nếu đã thay đổi source, chạy `docker-compose build --no-cache frontend` hoặc dùng `--no-cache`.

- Microphone không hoạt động:
    - Kiểm tra trình duyệt đã cấp quyền micro chưa.
    - Nếu trình duyệt không hỗ trợ Web Speech API, UI sẽ ẩn nút mic.
    - Triển khai production cần HTTPS để cấp quyền micro.

- Kết nối MQTT bị từ chối / Unknown host:
    - Trong Docker Compose, sử dụng tên service (ví dụ `mosquitto`) thay vì `localhost`.
    - Khi chạy cục bộ (không Docker), dùng `localhost` và đảm bảo Mosquitto đang chạy.

Mẹo phát triển
--------------
- Chạy Vite dev server (`npm run dev`) để phát triển frontend nhanh.
- Bật log debug cho `org.springframework.integration` và Paho trong Spring Boot nếu cần debug MQTT.
- AI service trả về JSON định dạng khi muốn thực hiện "tool call" — backend sẽ phân tích và thực hiện hành động tương ứng.

Góp ý & đóng góp
----------------
1. Tạo nhánh (branch) mới từ `main`.
2. Viết code, test và commit.
3. Mở Pull Request mô tả thay đổi.

Liên hệ
-------
Nếu cần hỗ trợ thêm, mở issue hoặc liên hệ người phụ trách dự án.


# Template
Đây là một file README.md chi tiết, mô tả toàn bộ dự án IoT Vườn Thông Minh, bao gồm kiến trúc, công nghệ, và hướng dẫn chạy thử.

-----

# Nền tảng IoT Vườn Thông Minh (Spring Boot & ESP32)

Dự án này là một nền tảng Internet of Things (IoT) toàn diện, được xây dựng để giám sát và điều khiển một hệ thống vườn thông minh trong thời gian thực.

Hệ thống bao gồm một **Backend Spring Boot** mạnh mẽ, giao tiếp không đồng bộ qua **MQTT** với các thiết bị **ESP32**, lưu trữ dữ liệu vào **MySQL**, sử dụng **Redis** để caching trạng thái, và cung cấp một **API** để gửi lệnh điều khiển.

Một tính năng nổi bật là việc tích hợp **Spring AI**, cho phép người dùng hỏi-đáp và ra lệnh cho khu vườn bằng ngôn ngữ tự nhiên.

## 1\. Kiến trúc Hệ thống 🏛️

Hệ thống được thiết kế theo kiến trúc hướng sự kiện (Event-Driven) với hai luồng hoạt động chính:

1.  **Luồng Dữ liệu (Device-to-Cloud):** Dữ liệu từ cảm biến được gửi không đồng bộ.
    `ESP32 -> MQTT Broker (Mosquitto) -> Spring Boot (Integration) -> Redis (Cache) & MySQL (Log) -> WebSocket -> Dashboard`

2.  **Luồng Điều khiển (User-to-Device):** Lệnh từ người dùng được gửi xuống thiết bị.
    `User -> Spring Boot (REST API) -> Spring AI (Xử lý lệnh) -> MQTT Broker -> ESP32`

-----

## 2\. Tính năng Chính ⭐

* **Giám sát Thời gian thực:** Cập nhật liên tục nhiệt độ, độ ẩm không khí, độ ẩm đất, và cường độ ánh sáng.
* **Điều khiển Từ xa:** Bật/tắt máy bơm nước thông qua REST API.
* **Logic Tưới Tự động:** Firmware của ESP32 tự động tưới dựa trên độ ẩm đất và ánh sáng (chỉ khi ở chế độ `AUTO`).
* **Lưu trữ Lịch sử:** Toàn bộ dữ liệu cảm biến được lưu trữ vào MySQL để phân tích sau này (quản lý schema bằng **Flyway**).
* **Caching Hiệu năng cao:** Trạng thái *tức thời* của mọi thiết bị được cache trên **Redis**, giúp truy vấn API (`/state`) cực nhanh.
* **Trợ lý AI (Spring AI):**
    * Hỏi-đáp về trạng thái vườn ("Nhiệt độ hôm nay thế nào?").
    * Điều khiển bằng ngôn ngữ tự nhiên ("Bật máy bơm cho tôi").
* **Tài liệu API (Swagger):** Tích hợp OpenAPI 3 để tài liệu hóa và kiểm thử API một cách trực quan.

-----

## 3\. Công nghệ sử dụng 🛠️

| Lĩnh vực | Công nghệ |
| :--- | :--- |
| **Backend** | Spring Boot, Spring Integration (MQTT), Spring Data JPA, Spring Cache (Redis), Spring WebSocket (STOMP), Spring AI, MapStruct |
| **Cơ sở dữ liệu** | MySQL (Lưu trữ lịch sử), Flyway (Quản lý Schema) |
| **Cache** | Redis (Lưu trạng thái tức thời) |
| **Broker** | Mosquitto (Broker MQTT cục bộ, có xác thực) |
| **Thiết bị (Firmware)** | C++ (Arduino IDE), ESP32 |
| **Giao thức** | MQTT, TCP/IP, HTTP/REST, WebSocket |
| **Thư viện Arduino** | `PubSubClient`, `ArduinoJson`, `DHT`, `BH1750` |
| **Tài liệu API** | OpenAPI 3 (Springdoc / Swagger UI) |

-----

## 4\. Hướng dẫn Cài đặt & Chạy thử (Getting Started)

### Yêu cầu Tiên quyết

1.  **Java JDK 17+** và **Maven 3+**
2.  **Cơ sở dữ liệu MySQL** (ví dụ: `smart_garden_db`)
3.  **Redis Server** (chạy ở port 6379)
4.  **Mosquitto MQTT Broker** (chạy ở port 1883)
5.  **Arduino IDE** và **ESP32**
6.  **MQTTX** (hoặc MQTT Explorer) và **Postman** (để kiểm thử)

### Bước 1: Cấu hình Mosquitto Broker (Rất quan trọng)

Backend được cấu hình để kết nối đến `localhost:1883` với `username: iot_admin` và `password: 123456`.

1.  Cài đặt Mosquitto.
2.  Tạo file `password.txt` với nội dung: `iot_admin:123456`
3.  Chạy lệnh `mosquitto_passwd -U password.txt` để băm mật khẩu.
4.  Tạo file `mosquitto.conf` với nội dung:
    ```ini
    allow_anonymous false
    password_file /đường/dẫn/tới/file/password.txt
    listener 1883
    ```
5.  Chạy Mosquitto: `mosquitto -c mosquitto.conf`

### Bước 2: Cấu hình Backend (Spring Boot)

1.  Clone repository.
2.  Mở `src/main/resources/application.yml`.
3.  Cập nhật thông tin `spring.datasource` (username/password MySQL của bạn).
4.  Đảm bảo `spring.redis` và `mqtt` trỏ đúng (thường là `localhost`).
5.  Thêm API Key của bạn vào `spring.ai.openai.api-key` (nếu bạn muốn test AI).

### Bước 3: Chạy Backend

Mở terminal và chạy:

```bash
mvn spring-boot:run
```

**Flyway** sẽ tự động chạy và tạo các bảng CSDL (từ `V1__...Schema.sql`). Backend sẽ kết nối tới Mosquitto.

### Bước 4: Nạp Firmware (ESP32)

1.  Mở file `.ino` bằng Arduino IDE.
2.  Cài đặt các thư viện: `PubSubClient`, `ArduinoJson`, `DHT`, `BH1750`.
3.  Cập nhật các thông số sau trong code:
    ```cpp
    const char* WIFI_SSID = "TEN_WIFI_CUA_BAN";
    const char* WIFI_PASS = "MAT_KHAU_WIFI";
    const char* DEVICE_UID = "ESP32_GARDEN_01"; 

    // Đảm bảo khớp với Mosquitto
    const char* MQTT_USER = "iot_admin";
    const char* MQTT_PASS = "123456";
    ```
4.  Cắm ESP32, chọn đúng cổng COM và nạp code.

-----

## 5\. Hướng dẫn Test (Không cần Frontend)

### Bước 1: Kết nối MQTTX (Giả lập Client)

1.  Mở MQTTX, tạo kết nối mới.
2.  **Host:** `localhost`
3.  **Port:** `1883`
4.  **Username:** `iot_admin`
5.  **Password:** `123456`
6.  Nhấn **Connect**.

### Bước 2: Đăng ký Thiết bị (Postman)

Để backend biết về thiết bị này:

* `POST http://localhost:8080/api/v1/devices`
* Body (JSON):
  ```json
  {
      "deviceUid": "ESP32_GARDEN_01",
      "name": "Vườn Thử Nghiệm"
  }
  ```

### Bước 3: Kiểm tra Luồng Data (ESP32 -\> API)

1.  Mở Serial Monitor trong Arduino IDE. Bạn sẽ thấy ESP32 kết nối và bắt đầu gửi `telemetry` và `state`.
2.  Vào **Postman**, gọi:
    `GET http://localhost:8080/api/v1/devices/ESP32_GARDEN_01/state`
3.  **Kết quả:** Bạn sẽ thấy JSON trạng thái tức thời (từ Redis) mà ESP32 vừa gửi.
    ```json
    {
        "deviceUid": "ESP32_GARDEN_01",
        "status": "online",
        "controlMode": "AUTO",
        "pumpState": "OFF",
        "sensors": { ... }
    }
    ```

### Bước 4: Kiểm tra Luồng Điều khiển (API -\> ESP32)

1.  Trong **MQTTX**, subscribe (đăng ký) vào topic: `smartgarden/device/ESP32_GARDEN_01/command`
2.  Trong **Postman**, gửi một lệnh:
    * `POST http://localhost:8080/api/v1/devices/ESP32_GARDEN_01/command`
    * Body (JSON):
      ```json
      {
          "action": "SET_MODE",
          "payload": {
              "value": "MANUAL"
          }
      }
      ```
3.  **Kết quả:** Ngay lập tức, bạn sẽ thấy tin nhắn lệnh này xuất hiện trong **MQTTX**. ESP32 (nếu đang chạy) cũng sẽ nhận được và chuyển sang chế độ `MANUAL`.

### Bước 5: Kiểm tra AI (Postman)

* `POST http://localhost:8080/api/v1/ai/chat`
* Body (JSON):
  ```json
  {
      "message": "Nhiệt độ vườn của tôi là bao nhiêu?"
  }
  ```
* **Kết quả:** AI sẽ trả lời (ví dụ: "Nhiệt độ hiện tại là 28.5°C.") sau khi tự động gọi hàm `getDeviceState` nội bộ.

-----

## 6\. Tài liệu API (Swagger)

Khi backend đang chạy, bạn có thể truy cập tài liệu API tương tác tại:

**`http://localhost:8080/swagger-ui.html`**
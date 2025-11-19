# Hướng dẫn chạy Project Smart Garden trên IntelliJ IDEA

## 📋 Mục lục
1. [Cài đặt các công cụ cần thiết](#1-cài-đặt-các-công-cụ-cần-thiết)
2. [Import Project vào IntelliJ](#2-import-project-vào-intellij)
3. [Chạy bằng Docker (Khuyến nghị - Dễ nhất)](#3-chạy-bằng-docker-khuyến-nghị---dễ-nhất)
4. [Chạy thủ công (Không dùng Docker)](#4-chạy-thủ-công-không-dùng-docker)
5. [Kiểm tra kết quả](#5-kiểm-tra-kết-quả)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Cài đặt các công cụ cần thiết

### ✅ Bắt buộc phải có:

1. **Java JDK 21** (hoặc JDK 17+)
   - Download: https://www.oracle.com/java/technologies/downloads/
   - Hoặc dùng OpenJDK: https://adoptium.net/
   - Kiểm tra: Mở PowerShell, gõ `java -version`

2. **IntelliJ IDEA** (Community hoặc Ultimate)
   - Download: https://www.jetbrains.com/idea/download/
   - Community Edition (miễn phí) là đủ

3. **Git** (để clone project)
   - Download: https://git-scm.com/downloads

### 🔧 Tùy chọn (tùy cách chạy):

**OPTION A: Chạy bằng Docker (Khuyến nghị)**
- **Docker Desktop** (bao gồm Docker Compose)
  - Download: https://www.docker.com/products/docker-desktop/
  - Sau khi cài, restart máy và mở Docker Desktop để nó chạy

**OPTION B: Chạy thủ công (Không dùng Docker)**
- **MySQL 8.0+**
  - Download: https://dev.mysql.com/downloads/installer/
  - Hoặc dùng XAMPP: https://www.apachefriends.org/
- **Redis**
  - Download: https://github.com/tporadowski/redis/releases (Windows)
- **Mosquitto MQTT Broker**
  - Download: https://mosquitto.org/download/

---

## 2. Import Project vào IntelliJ

### Bước 1: Mở IntelliJ IDEA

### Bước 2: Import Project
1. Click **File** → **Open**
2. Chọn thư mục project: `d:\Year4_Semester 1\IoT\BTL\Template`
3. Click **OK**

### Bước 3: Đợi IntelliJ index project
- IntelliJ sẽ tự động:
  - Phát hiện đây là Maven project
  - Download các dependencies trong `pom.xml`
  - Index code
- Quá trình này mất 2-5 phút tùy tốc độ mạng
- Xem progress ở góc dưới bên phải

### Bước 4: Cấu hình JDK
1. Click **File** → **Project Structure** (Ctrl+Alt+Shift+S)
2. Trong tab **Project**:
   - **SDK**: Chọn Java 21 (hoặc 17+)
   - Nếu chưa có, click **Add SDK** → **Download JDK** → Chọn version 21
3. Trong tab **Modules**:
   - Đảm bảo **Language level** là **21 - Record patterns, pattern matching for switch**
4. Click **OK**

### Bước 5: Enable Maven Auto-Import
1. Click **File** → **Settings** (Ctrl+Alt+S)
2. Tìm kiếm: **Maven**
3. Trong **Maven** → **Importing**:
   - ✅ Tick vào **Import Maven projects automatically**
4. Click **OK**

---

## 3. Chạy bằng Docker (Khuyến nghị - Dễ nhất)

### ⭐ Ưu điểm:
- Không cần cài MySQL, Redis, Mosquitto riêng
- Tất cả services chạy cùng lúc
- Cấu hình sẵn, ít lỗi

### Bước 1: Đảm bảo Docker Desktop đang chạy
- Mở Docker Desktop
- Đợi icon Docker ở System Tray (góc dưới phải) chuyển sang màu xanh

### Bước 2: Mở Terminal trong IntelliJ
- Click **View** → **Tool Windows** → **Terminal**
- Hoặc nhấn **Alt+F12**

### Bước 3: Chạy lệnh Docker Compose
```powershell
# Chạy tất cả services (MySQL, Redis, Mosquitto, Backend, AI)
docker-compose up --build -d
```

**Giải thích:**
- `up`: Khởi động services
- `--build`: Build lại images nếu có thay đổi code
- `-d`: Chạy ngầm (detached mode)

### Bước 4: Kiểm tra trạng thái
```powershell
# Xem các container đang chạy
docker-compose ps
```

Bạn sẽ thấy:
```
NAME                  STATUS
mqtt_broker           Up
mysql_db              Up (healthy)
redis_cache           Up (healthy)
smart_garden_app      Up
python-ai             Up (healthy)
```

### Bước 5: Xem logs (nếu cần debug)
```powershell
# Xem tất cả logs
docker-compose logs -f

# Xem logs của backend
docker-compose logs -f app

# Xem logs của MySQL
docker-compose logs -f mysql
```

### Bước 6: Dừng các services
```powershell
# Dừng và xóa containers
docker-compose down

# Dừng và xóa cả volumes (data)
docker-compose down -v
```

---

## 4. Chạy thủ công (Không dùng Docker)

### Bước 1: Khởi động MySQL

**Nếu dùng XAMPP:**
1. Mở XAMPP Control Panel
2. Start **MySQL**

**Nếu dùng MySQL standalone:**
1. Mở **Services** (Win+R → `services.msc`)
2. Tìm **MySQL80**, click **Start**

**Tạo database:**
```sql
-- Mở MySQL Workbench hoặc phpMyAdmin
CREATE DATABASE IF NOT EXISTS iot_db;
```

### Bước 2: Khởi động Redis

**Windows:**
```powershell
# Di chuyển đến thư mục cài Redis
cd "C:\Program Files\Redis"

# Chạy Redis server
redis-server.exe
```

**Kiểm tra Redis:**
```powershell
# Mở terminal mới
redis-cli ping
# Phải trả về: PONG
```

### Bước 3: Khởi động Mosquitto MQTT Broker

**Cấu hình Mosquitto:**
1. Mở thư mục: `mqtt_broker\mosquitto\config\`
2. File `mosquitto.conf` đã có sẵn
3. File `pwfile` đã có sẵn (username: iot_admin, password: 123456)

**Chạy Mosquitto:**
```powershell
# Option 1: Chạy từ thư mục project
cd mqtt_broker\mosquitto
mosquitto -c config\mosquitto.conf -v

# Option 2: Nếu Mosquitto đã cài vào System
# Mở Services, Start "Mosquitto Broker"
```

### Bước 4: Cấu hình application.properties

Mở file: `src/main/resources/application.properties`

Kiểm tra các cấu hình sau:

```properties
# MySQL (thay đổi nếu cần)
spring.datasource.url=jdbc:mysql://localhost:13306/iot_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# MQTT
mqtt.broker-url=tcp://localhost:1883
mqtt.username=iot_admin
mqtt.password=123456
```

**⚠️ Lưu ý:**
- Nếu MySQL chạy ở port mặc định 3306 (không phải Docker), đổi `13306` → `3306`
- Thay `root` password thành password MySQL của bạn

### Bước 5: Chạy Backend trong IntelliJ

**Cách 1: Dùng Maven**
1. Mở Terminal trong IntelliJ (Alt+F12)
2. Chạy lệnh:
```powershell
.\mvnw spring-boot:run
```

**Cách 2: Dùng Run Configuration**
1. Tìm file: `src/main/java/com/example/demo/DemoApplication.java`
2. Click chuột phải vào file
3. Chọn **Run 'DemoApplication'** (Shift+F10)

**Cách 3: Tạo Run Configuration (Khuyến nghị)**
1. Click **Run** → **Edit Configurations**
2. Click **+** → **Application**
3. Cấu hình:
   - **Name**: Smart Garden
   - **Main class**: `com.example.demo.DemoApplication`
   - **JRE**: Java 21
   - **Module**: demo
4. Click **OK**
5. Click nút ▶️ (Run) trên toolbar

### Bước 6: Chạy AI Service (Optional)

**Nếu muốn dùng AI chat:**

1. Mở Terminal, di chuyển đến thư mục AI:
```powershell
cd AI
```

2. Cài đặt dependencies Python:
```powershell
pip install -r requirements.txt
```

3. Chạy AI service:
```powershell
python -m uvicorn AIService:app --host 0.0.0.0 --port 8000 --reload
```

**Nếu KHÔNG dùng AI:**
- Bỏ qua bước này, backend vẫn chạy bình thường
- Chỉ API `/api/ai/chat` sẽ không hoạt động

---

## 5. Kiểm tra kết quả

### ✅ Backend đã chạy thành công khi:

1. **Console hiển thị:**
```
Started DemoApplication in X.XXX seconds
```

2. **Không có lỗi đỏ** trong console

3. **Truy cập được Swagger UI:**
   - Mở trình duyệt
   - Vào: http://localhost:8080/swagger-ui.html
   - Sẽ thấy giao diện API documentation

### 🧪 Test các chức năng:

#### Test 1: API Health Check
```
GET http://localhost:8080/
```
- Dùng Postman hoặc browser
- Sẽ thấy trang `index.html` hoặc status OK

#### Test 2: Swagger API
1. Mở: http://localhost:8080/swagger-ui.html
2. Thử API **Device Controller** → **POST /api/devices**
3. Click **Try it out**
4. Nhập:
```json
{
  "deviceUid": "ESP32_TEST_001",
  "name": "Thiết bị test"
}
```
5. Click **Execute**
6. Xem response code **200** → Thành công

#### Test 3: MQTT Connection
1. Download **MQTTX**: https://mqttx.app/
2. Tạo kết nối mới:
   - **Name**: Smart Garden
   - **Host**: localhost
   - **Port**: 1883 (hoặc 18883 nếu dùng Docker)
   - **Username**: iot_admin
   - **Password**: 123456
3. Click **Connect**
4. Status hiển thị **Connected** → Thành công

#### Test 4: Database (MySQL)
1. Mở MySQL Workbench hoặc phpMyAdmin
2. Kết nối đến:
   - **Host**: localhost
   - **Port**: 3306 (hoặc 13306 nếu dùng Docker)
   - **Username**: root
   - **Password**: root
3. Xem database `iot_db`
4. Kiểm tra các bảng:
   - `device_entity`
   - `telemetry_log`
   - `flyway_schema_history`

#### Test 5: Redis
```powershell
# Mở terminal
redis-cli

# Trong redis-cli, gõ:
PING
# Trả về: PONG

# Kiểm tra keys
KEYS *

# Thoát
exit
```

---

## 6. Troubleshooting

### ❌ Lỗi: "Port 8080 already in use"

**Nguyên nhân:** Có ứng dụng khác đang dùng port 8080

**Giải pháp:**
1. Tìm process đang dùng port 8080:
```powershell
netstat -ano | findstr :8080
```

2. Kill process (thay PID bằng số thực tế):
```powershell
taskkill /PID <PID> /F
```

3. Hoặc đổi port trong `application.properties`:
```properties
server.port=8081
```

---

### ❌ Lỗi: "Unable to obtain JDBC Connection"

**Nguyên nhân:** Không kết nối được MySQL

**Giải pháp:**
1. Kiểm tra MySQL đã chạy chưa:
```powershell
# Mở Services
services.msc
# Tìm MySQL80 → Status phải là "Running"
```

2. Kiểm tra port MySQL:
```powershell
netstat -ano | findstr :3306
```

3. Kiểm tra username/password trong `application.properties`

4. Test kết nối bằng MySQL Workbench

---

### ❌ Lỗi: "Unable to connect to Redis"

**Nguyên nhân:** Redis chưa chạy

**Giải pháp:**
1. Khởi động Redis:
```powershell
redis-server.exe
```

2. Test kết nối:
```powershell
redis-cli ping
```

3. Nếu không có Redis, tạm thời disable cache:
```properties
# Trong application.properties
spring.cache.type=none
```

---

### ❌ Lỗi: "MQTT connection failed"

**Nguyên nhân:** Mosquitto chưa chạy hoặc cấu hình sai

**Giải pháp:**
1. Kiểm tra Mosquitto đã chạy:
```powershell
netstat -ano | findstr :1883
```

2. Test kết nối bằng MQTTX (xem Test 3 ở trên)

3. Kiểm tra username/password trong `application.properties`:
```properties
mqtt.username=iot_admin
mqtt.password=123456
```

4. Kiểm tra file `mqtt_broker/mosquitto/config/mosquitto.conf`

---

### ❌ Lỗi: "Java version mismatch"

**Nguyên nhân:** Dùng sai version Java

**Giải pháp:**
1. Kiểm tra Java version:
```powershell
java -version
```

2. Phải là Java 17 hoặc 21

3. Nếu sai, download đúng version: https://adoptium.net/

4. Cấu hình lại trong IntelliJ:
   - **File** → **Project Structure** → **Project** → **SDK**

---

### ❌ Lỗi: "Maven dependencies not found"

**Nguyên nhân:** Maven chưa download dependencies

**Giải pháp:**
1. Trong IntelliJ, click chuột phải vào `pom.xml`
2. Chọn **Maven** → **Reload Project**
3. Hoặc chạy:
```powershell
.\mvnw clean install
```

---

### ❌ Docker: "Cannot connect to Docker daemon"

**Nguyên nhân:** Docker Desktop chưa chạy

**Giải pháp:**
1. Mở Docker Desktop
2. Đợi icon Docker ở System Tray chuyển sang màu xanh
3. Chạy lại `docker-compose up`

---

### ❌ Docker: "Port is already allocated"

**Nguyên nhân:** Port đã được dùng bởi service khác

**Giải pháp:**
1. Dừng service đang dùng port đó (MySQL, Redis, Mosquitto local)
2. Hoặc đổi port mapping trong `docker-compose.yml`:
```yaml
# Ví dụ đổi MySQL port
ports:
  - "13307:3306"  # Thay vì 13306
```

---

## 📚 Tài liệu tham khảo

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **README chính**: [README.md](./README.md)
- **Code Arduino**: [ESP32_SmartGarden/README.md](./ESP32_SmartGarden/README.md)
- **Kiến trúc hệ thống**: [ESP32_SmartGarden/ARCHITECTURE.md](./ESP32_SmartGarden/ARCHITECTURE.md)

---

## 🎯 Tóm tắt các lệnh quan trọng

### Chạy bằng Docker:
```powershell
# Khởi động tất cả
docker-compose up -d

# Xem logs
docker-compose logs -f app

# Dừng tất cả
docker-compose down
```

### Chạy thủ công:
```powershell
# Backend (Maven)
.\mvnw spring-boot:run

# AI Service
cd AI
python -m uvicorn AIService:app --host 0.0.0.0 --port 8000 --reload
```

### Kiểm tra services:
```powershell
# MySQL
netstat -ano | findstr :3306

# Redis
redis-cli ping

# Mosquitto
netstat -ano | findstr :1883

# Backend
curl http://localhost:8080
```

---

## 💡 Tips

1. **Enable Auto-Reload trong IntelliJ:**
   - **File** → **Settings** → **Build, Execution, Deployment** → **Compiler**
   - ✅ Tick **Build project automatically**
   - Nhấn **Ctrl+Shift+A** → Tìm **Registry**
   - ✅ Tick `compiler.automake.allow.when.app.running`

2. **Xem Console đẹp hơn:**
   - **Run** → **Edit Configurations** → **Modify options** → ✅ **Color output**

3. **Debug:**
   - Đặt breakpoint bằng cách click vào lề trái (bên số dòng)
   - Chạy Debug mode: **Shift+F9**

4. **Hot Reload:**
   - Thêm Spring DevTools vào `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-devtools</artifactId>
       <optional>true</optional>
   </dependency>
   ```

---

Chúc bạn chạy project thành công! 🎉

Nếu gặp lỗi, tham khảo phần **Troubleshooting** hoặc liên hệ team.

# Smart Garden Frontend - React.js

Frontend riêng cho hệ thống Smart Garden IoT, sử dụng React.js để phát triển local.

## Cài đặt

```bash
cd frontend
npm install
```

## Chạy local

```bash
npm start
```

Ứng dụng sẽ chạy tại `http://localhost:3000`

## Cấu trúc dự án

```
frontend/
├── public/
│   └── index.html          # HTML template
├── src/
│   ├── components/         # Các component tái sử dụng
│   │   └── Navbar.js       # Navigation bar
│   ├── pages/              # Các trang chính
│   │   ├── Home.js         # Trang chủ
│   │   ├── Control.js      # Điều khiển thiết bị
│   │   ├── Dashboard.js    # Thống kê dữ liệu
│   │   └── Chat.js         # AI Chat
│   ├── services/           # API và WebSocket services
│   │   ├── api.js          # REST API client
│   │   └── websocket.js    # WebSocket client
│   ├── App.js              # Root component với routing
│   └── index.js            # Entry point
├── .env                    # Environment variables
└── package.json            # Dependencies
```

## Cấu hình

File `.env` chứa cấu hình kết nối:

```
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=http://localhost:8080/ws
REACT_APP_DEVICE_UID=ESP32_GARDEN_001
```

## Tính năng

- ✅ **Điều khiển thiết bị**: Bật/tắt máy bơm, chuyển chế độ MANUAL/AUTO
- ✅ **Giám sát real-time**: WebSocket + Polling fallback (mỗi 3s)
- ✅ **Hiển thị cảm biến**: Nhiệt độ, độ ẩm không khí, độ ẩm đất
- ⏳ **Thống kê**: Biểu đồ lịch sử (sẽ triển khai với Chart.js)
- ⏳ **AI Chat**: Trợ lý AI với Gemini (sẽ triển khai)

## API Backend

Backend chạy tại `http://localhost:8080`

### REST Endpoints:
- `GET /api/v1/devices` - Danh sách thiết bị
- `GET /api/v1/devices/{uid}/state` - Trạng thái hiện tại
- `POST /api/v1/devices/{uid}/command` - Gửi lệnh điều khiển
- `POST /api/v1/devices/{uid}/auto-off?autoOff={boolean}` - Đặt chế độ
- `GET /api/v1/devices/{uid}/history?from={timestamp}&to={timestamp}` - Lịch sử

### WebSocket:
- **Endpoint**: `ws://localhost:8080/ws`
- **Protocol**: STOMP over SockJS
- **Topics**:
  - `/topic/device/{deviceUid}` - Cập nhật theo thiết bị
  - `/topic/all-devices` - Broadcast tất cả thiết bị

## Build production

```bash
npm run build
```

Build output sẽ được tạo trong folder `build/`

## Ghi chú

- WebSocket sử dụng STOMP over SockJS với fallback polling mỗi 3 giây
- UI sử dụng Tailwind CSS qua CDN (trong index.html)
- Hỗ trợ hot-reload khi phát triển local
- CORS đã được cấu hình ở backend để cho phép kết nối từ React dev server

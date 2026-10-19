# Classroom IoT

Ứng dụng theo dõi nhiệt độ, độ ẩm, ánh sáng và điều khiển ba LED.

## Chạy local

Backend yêu cầu Java 25 (bản LTS hiện tại) và Maven Wrapper.

1. Khởi động MySQL và tạo database `classroom_iot`. Backend dùng Flyway để tạo bảng. Có thể ghi đè `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` bằng biến môi trường.
2. Khởi động backend:

   ```powershell
   cd BE
   $env:BOOTSTRAP_USERNAME = 'admin'
   $env:BOOTSTRAP_PASSWORD = '<mật-khẩu-local>'
   .\mvnw.cmd spring-boot:run
   ```

   Lần khởi động đầu tạo tài khoản bootstrap nếu username chưa tồn tại. API chạy ở `http://localhost:8080`.
3. Nếu dùng MQTT, cấu hình `MQTT_BROKER_URL`, `MQTT_USERNAME` và `MQTT_PASSWORD` theo broker. Mặc định topic khớp sketch trong `HW/sketch.ino`. Khi broker chưa kết nối, các API đọc dữ liệu vẫn chạy nhưng dashboard báo MQTT offline và lệnh LED không thể được xác nhận.
4. Trong terminal khác chạy FE:

   ```powershell
   cd FE
   Copy-Item .env.example .env.local
   pnpm install
   pnpm dev
   ```

   Mặc định FE gọi backend tại `http://localhost:8080`; sửa `VITE_API_BASE_URL` trong `.env.local` nếu backend chạy ở địa chỉ khác. Mở `http://localhost:5173`, đăng nhập bằng tài khoản bootstrap.

FE lưu access token trong `sessionStorage`; WebSocket STOMP xác thực JWT và chỉ subscribe các topic realtime được hỗ trợ.

REST response dùng envelope thống nhất `{ success, code, message, data }`; schema OpenAPI có tại `/v3/api-docs` và giao diện Swagger tại `/swagger-ui.html`.

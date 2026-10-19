# API Documentation — Classroom IoT

## 1. Thông tin chung

- **Base URL:** `http://localhost:8080`
- **API prefix:** `/api`
- **Content-Type:** `application/json; charset=utf-8` (trừ API tải ảnh đại diện dùng `multipart/form-data`)
- **Authentication:** JWT Bearer Token, truyền trong header `Authorization: Bearer <accessToken>`.
- **Múi giờ dữ liệu:** `Asia/Ho_Chi_Minh` (UTC+07:00).
- **Response thành công:** `{ "success": true, "code": "IOT-000", "message": "Thành công", "data": ... }`.

Các API được đánh dấu **Authenticated** yêu cầu JWT hợp lệ. `/api/auth/login` là public. WebSocket cho phép mở handshake công khai, nhưng STOMP `CONNECT` bắt buộc có Bearer token và chỉ được subscribe các topic được liệt kê ở phần WebSocket.

## 2. Danh sách endpoint

| STT | Method | Endpoint | Chức năng | Quyền |
|---:|---|---|---|---|
| 1 | POST | `/api/auth/login` | Đăng nhập, trả JWT và thông tin người dùng | Public |
| 2 | GET | `/api/auth/profile/{username}` | Lấy hồ sơ của người dùng đang đăng nhập | Authenticated |
| 3 | PUT | `/api/auth/profile/{id}` | Cập nhật hồ sơ và URL tài nguyên | Authenticated |
| 4 | POST | `/api/auth/profile/{id}/avatar` | Tải ảnh đại diện PNG/JPG | Authenticated |
| 5 | GET | `/api/sensor/latest` | Lấy giá trị mới nhất của nhiệt độ, độ ẩm, ánh sáng | Authenticated |
| 6 | GET | `/api/sensor/chart` | Lấy điểm dữ liệu trong khoảng giờ/phút gần đây | Authenticated |
| 7 | GET | `/api/sensor` | Tìm kiếm, lọc và phân trang dữ liệu cảm biến | Authenticated |
| 8 | GET | `/api/devices` | Lấy trạng thái LED1–LED3 và trạng thái online | Authenticated |
| 9 | POST | `/api/devices/control` | Gửi lệnh điều khiển LED | Authenticated |
| 10 | GET | `/api/devices/mqtt-status` | Kiểm tra backend có kết nối MQTT broker không | Authenticated |
| 11 | GET | `/api/history` | Tra cứu lịch sử lệnh điều khiển | Authenticated |
| 12 | STOMP over WebSocket | `/ws` | Nhận dữ liệu cảm biến, trạng thái thiết bị, lịch sử lệnh theo thời gian thực | STOMP CONNECT cần JWT |

## 3. Định dạng dùng chung

### Response thành công

```json
{
  "success": true,
  "code": "IOT-000",
  "message": "Thành công",
  "data": {}
}
```

### Response lỗi

```json
{
  "success": false,
  "code": "IOT-400",
  "message": "Dữ liệu đầu vào không hợp lệ",
  "data": {
    "timestamp": "2026-10-20T20:00:00+07:00",
    "status": 400,
    "path": "/api/sensor",
    "details": {}
  }
}
```

Lỗi xác thực thường trả `401 IOT-401`; không có quyền trả `403 IOT-403`. Các lỗi nghiệp vụ dùng HTTP status và mã `IOT-*` tương ứng, ví dụ thiết bị ngoại tuyến trả `503 IOT-503`.

## 4. Chi tiết API

### API-01 — Đăng nhập

`POST /api/auth/login` — Public

**Request body**

```json
{
  "username": "admin",
  "password": "<password>"
}
```

Hai trường bắt buộc. Đăng nhập thành công trả `200 OK`:

```json
{
  "success": true,
  "code": "IOT-000",
  "message": "Thành công",
  "data": {
    "accessToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userData": {
      "id": 1,
      "username": "admin",
      "fullName": "Admin",
      "studentCode": null,
      "className": null,
      "email": null,
      "avatarUrl": null,
      "githubUrl": null,
      "figmaUrl": null,
      "postmanUrl": null,
      "reportUrl": null
    }
  }
}
```

`401 IOT-401`: sai username/password. `400 IOT-400`: thiếu trường bắt buộc.

### API-02 — Lấy hồ sơ

`GET /api/auth/profile/{username}` — Authenticated

Token chỉ được lấy hồ sơ của chính username trong token (không phân biệt hoa/thường).

**Response `data`** là `UserData`: `id`, `username`, `fullName`, `studentCode`, `className`, `email`, `avatarUrl`, `githubUrl`, `figmaUrl`, `postmanUrl`, `reportUrl`.

`403 IOT-403`: username không thuộc token; `404 IOT-404`: không tìm thấy user.

### API-03 — Cập nhật hồ sơ

`PUT /api/auth/profile/{id}` — Authenticated

`id` phải là ID của user trong token.

```json
{
  "fullName": "Nguyễn Văn A",
  "studentCode": "B23DCCN001",
  "className": "D23CQCN01-B",
  "email": "a@example.com",
  "avatarUrl": null,
  "githubUrl": "https://github.com/example",
  "figmaUrl": "https://figma.com/file/example",
  "postmanUrl": "https://www.postman.com/example",
  "reportUrl": "https://drive.google.com/example"
}
```

`fullName` và `studentCode` bắt buộc; `email` nếu có phải đúng định dạng. Trả `200 OK` với `data` là hồ sơ đã cập nhật. `403 IOT-403` nếu cập nhật ID người khác.

### API-04 — Tải ảnh đại diện

`POST /api/auth/profile/{id}/avatar` — Authenticated, `multipart/form-data`

- Form field: `file` (một ảnh PNG hoặc JPEG).
- Kích thước file tối đa **20 MB**, chiều rộng/cao tối đa **10.000 px**.
- Backend thu nhỏ ảnh tối đa 512 px, lưu ảnh và trả `UserData` với `avatarUrl` mới.
- Ảnh được phục vụ tại `/uploads/avatars/...`; URL này public để trình duyệt tải ảnh.

`400 IOT-400`: file rỗng, không đúng định dạng, quá kích thước hoặc ảnh không hợp lệ. `403 IOT-403`: ID không thuộc user hiện tại.

### API-05 — Lấy dữ liệu cảm biến mới nhất

`GET /api/sensor/latest` — Authenticated

Không có query parameter. `data` gồm `temperature`, `humidity`, `light` (mỗi giá trị có thể là `null`) và `lastUpdatedAt`:

```json
{
  "success": true,
  "code": "IOT-000",
  "message": "Thành công",
  "data": {
    "temperature": { "value": 28.5, "unit": "°C", "status": "NORMAL", "recordedAt": "2026-10-20T20:00:00+07:00" },
    "humidity": { "value": 60, "unit": "%", "status": "NORMAL", "recordedAt": "2026-10-20T20:00:00+07:00" },
    "light": { "value": 300, "unit": "lux", "status": "NORMAL", "recordedAt": "2026-10-20T20:00:00+07:00" },
    "lastUpdatedAt": "2026-10-20T20:00:00+07:00"
  }
}
```

`status` hiện có `NORMAL` hoặc `OUT_OF_RANGE`.

### API-06 — Dữ liệu biểu đồ

`GET /api/sensor/chart` — Authenticated

Chọn một trong hai query parameter:

| Tham số | Mô tả | Giới hạn | Mặc định |
|---|---|---:|---:|
| `hours` | Số giờ gần nhất | 1–168 | 1 giờ |
| `minutes` | Số phút gần nhất | 1–10080 | Không có |

Nếu truyền `minutes`, API ưu tiên tham số này. Ví dụ Dashboard 10 phút: `GET /api/sensor/chart?minutes=10`; 1 giờ: `GET /api/sensor/chart?hours=1`.

`data` là mảng các điểm `{ id, temperature, humidity, light, time }`. `time` có offset múi giờ Việt Nam. Giá trị ngoài giới hạn trả `400 IOT-400`.

### API-07 — Tra cứu dữ liệu cảm biến

`GET /api/sensor` — Authenticated

| Tham số | Mô tả | Mặc định / giới hạn |
|---|---|---|
| `page` | Số trang, bắt đầu từ 1 | `1` |
| `page_size` | Số dòng mỗi trang | `15`, từ 1 đến 100 |
| `sensor_code` | Lọc loại cảm biến: `temperature`, `humidity`, `light` (cũng nhận `temp`, `humid`, `lux`) | Không lọc |
| `sensorKey` | Alias của `sensor_code` | Không lọc |
| `value_min`, `value_max` | Lọc khoảng giá trị | Không lọc |
| `min`, `max` | Alias của `value_min`, `value_max` | Không lọc |
| `from`, `to` | Lọc mốc thời gian | Không lọc |
| `sort_by` | `time`, `recorded_at`, `value`, `unit`, `id` | `time` |
| `sort_dir` | `asc` hoặc `desc` | `desc` |

Thời gian nhận ISO-8601 có offset (ví dụ `2026-10-20T20:00:00+07:00`) hoặc datetime local `yyyy-MM-ddTHH:mm:ss`. Ví dụ tìm nhiệt độ trong khoảng thời gian:

```text
GET /api/sensor?sensor_code=temperature&from=2026-10-20T19:00:00%2B07:00&to=2026-10-20T20:00:00%2B07:00&page=1&page_size=15
```

Kết quả `data` có cấu trúc `{ items, page, pageSize, totalItems, totalPages }`. API giới hạn truy vấn trong **10.000 bản ghi cảm biến mới nhất**; filter và phân trang áp dụng trong giới hạn này.

### API-08 — Danh sách thiết bị và trạng thái

`GET /api/devices` — Authenticated

Trả mảng thiết bị, mỗi phần tử có `id`, `deviceCode`, `name`, `location`, `description`, `status`, `lastConfirmedStatus`, `lastChangedAt`, `online`. `status` có thể là `ON`, `OFF`, `LOADING`; `lastConfirmedStatus` là trạng thái gần nhất được ESP xác nhận.

### API-09 — Điều khiển thiết bị

`POST /api/devices/control` — Authenticated

```json
{ "command": "LED1_ON" }
```

Lệnh hợp lệ: `LED1_ON`, `LED1_OFF`, `LED2_ON`, `LED2_OFF`, `LED3_ON`, `LED3_OFF`, `ALL_ON`, `ALL_OFF`. API ghi lịch sử và gửi lệnh qua MQTT; kết quả xác nhận có thể đến sau qua WebSocket. `data` gồm `command` đã chuẩn hóa và mảng `actions`.

`400 IOT-400`: command không hợp lệ. `409 IOT-409`: thiết bị đang xử lý lệnh trước. `503 IOT-503`: ESP đang ngoại tuyến.

### API-10 — Trạng thái kết nối MQTT

`GET /api/devices/mqtt-status` — Authenticated

`data` có dạng `{ "connected": true, "checkedAt": "2026-10-20T20:00:00+07:00" }`. `connected` phản ánh kết nối MQTT của backend tới broker.

### API-11 — Lịch sử điều khiển

`GET /api/history` — Authenticated

| Tham số | Mô tả | Mặc định / giới hạn |
|---|---|---|
| `page` | Trang, bắt đầu từ 1 | `1` |
| `page_size` | Số dòng mỗi trang | `15`, từ 1 đến 100 |
| `device` | Một hoặc nhiều mã, phân cách bằng dấu phẩy: `LED1,LED2,LED3` | Không lọc |
| `device_code` | Alias của `device` | Không lọc |
| `action` | `ON` hoặc `OFF` | Không lọc |
| `status` | `LOADING`, `SUCCESS`, `FAILED` | Không lọc |
| `performed_by` | Username người thực hiện | Không lọc |
| `username` | Alias của `performed_by` | Không lọc |
| `from`, `to` | Khoảng thời gian | Không lọc |
| `sort_by` | `time`, `id`, `device`, `performed_by`, `action`, `status`, `response_time` | `time` |
| `sort_dir` | `asc` hoặc `desc` | `desc` |

Thời gian nhận ISO-8601, datetime local hoặc ngày `yyyy-MM-dd`. Khi chỉ truyền ngày, `from` bắt đầu lúc 00:00 ngày đó; `to` bao gồm trọn ngày được chỉ định. `data` có dạng `{ items, page, pageSize, totalItems, totalPages }`. Mỗi item gồm `id`, `device`, `action`, `performedBy`, `responseTimeSeconds`, `status`, `time`, `responseTimeMs`, `respondedAt`, `errorMessage`, `batchId`.

### API-12 — WebSocket/STOMP thời gian thực

- **SockJS endpoint:** `http://localhost:8080/ws`
- Kết nối STOMP gửi native header `Authorization: Bearer <accessToken>` trong frame `CONNECT`.
- Client chỉ nhận subscribe được các destination sau:

| Destination | Nội dung |
|---|---|
| `/topic/sensor` | Snapshot cảm biến: `id`, `temperature`, `humidity`, `light`, `time`, `statuses` |
| `/topic/device-status` | Trạng thái LED: `deviceCode`, `status`, `lastChangedAt`, ... |
| `/topic/action-log` | Cập nhật log điều khiển: thiết bị, hành động, trạng thái và thời gian phản hồi |

Client gửi frame `SEND` không được hỗ trợ. Kết nối STOMP thiếu JWT, JWT hết hạn hoặc subscribe destination khác sẽ bị từ chối.

## 5. Phân trang

API cảm biến và lịch sử dùng cùng cấu trúc phân trang:

```json
{
  "items": [],
  "page": 1,
  "pageSize": 15,
  "totalItems": 0,
  "totalPages": 0
}
```

## 6. OpenAPI / Swagger UI

Backend cấu hình OpenAPI và cho phép truy cập tài liệu không cần JWT:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`


# Kế hoạch xây dựng Backend

## 1. Mục tiêu và phạm vi

Xây dựng backend cho hệ thống giám sát cảm biến và điều khiển 3 LED trong phòng học. Backend dùng Spring Boot, MySQL, MQTT và STOMP WebSocket; FE gọi REST để tải dữ liệu và nhận cập nhật realtime qua WebSocket.

Danh sách API trong phạm vi:

| # | Method | Endpoint | Mục đích |
|---:|---|---|---|
| 1 | POST | `/api/auth/login` | Đăng nhập, trả về thông tin người dùng và thông tin xác thực |
| 2 | GET | `/api/sensor/latest` | Giá trị và trạng thái cảm biến mới nhất |
| 3 | GET | `/api/sensor/chart?hours=1` | Chuỗi điểm dữ liệu để vẽ biểu đồ |
| 4 | GET | `/api/sensor` | Lịch sử cảm biến, lọc và phân trang |
| 5 | GET | `/api/devices` | Danh mục và trạng thái 3 LED |
| 6 | POST | `/api/devices/control` | Gửi lệnh điều khiển |
| 7 | GET | `/api/devices/mqtt-status` | Trạng thái kết nối MQTT Broker |
| 8 | GET | `/api/history` | Lịch sử lệnh, lọc và phân trang |
| 9 | WS | `/ws` | Kênh STOMP realtime |
| 10 | GET | `/api/auth/profile/{username}` | Đọc profile |
| 11 | PUT | `/api/auth/profile/{id}` | Cập nhật profile và URL liên kết |

Các API REST ngoài login cần xác thực. Thiết bị hiện dùng MQTT trực tiếp; không thêm API riêng cho IoT vào phạm vi 11 API này.

**Tiến độ:** Phase 1–7 đã được implement và compile. MySQL local đã nhận migration V1 qua Flyway, tạo 5 bảng nghiệp vụ cùng bảng migration history và seed 3 cảm biến/3 thiết bị. Kết nối broker và trao đổi MQTT thực tế chưa được xác nhận trong phiên này.

## 2. Hiện trạng và nguồn chuẩn

- `BE` hiện chỉ là Spring Boot skeleton, package đang là `com.example.demo`, chưa có controller, database, security hay MQTT.
- MySQL là database được chọn cho dự án. Tài liệu schema cũ mô tả PostgreSQL nên chỉ tham khảo nghiệp vụ, không dùng nguyên cú pháp hay kiểu dữ liệu.
- `HW/sketch.ino` là nguồn chuẩn cho chân phần cứng và hành vi firmware. `HW/diagram.json` hiện không khớp chân với sketch.
- Firmware hiện dùng topic `data_sensors`, `device_control`, `device_response`; payload cảm biến có `nhiet_do`, `do_am`, `anh_sang`, trong đó ánh sáng là chuỗi có hậu tố đơn vị. Lệnh/ACK dùng `deviceId`, `action`, `status`; chưa có `command_id`.
- Firmware hỗ trợ `deviceId` 1–3 cho LED1–LED3 và `deviceId` 0 để bật/tắt cả ba LED.
- FE hiện còn dữ liệu mẫu và lưu trạng thái/lịch sử/profile cục bộ; các API sẽ thay dần phần này.

## 3. Cấu trúc package dự kiến

Chuyển package ứng dụng từ `com.example.demo` sang `com.ptit.iot` để thống nhất với kiến trúc dưới đây. Cập nhật class khởi động và cấu hình build liên quan khi bắt đầu triển khai.

```text
com.ptit.iot
├── IotApplication.java
├── controller
│   ├── AuthController.java
│   ├── SensorController.java
│   ├── DeviceController.java
│   └── HistoryController.java
├── service
│   ├── AuthService.java
│   ├── SensorService.java
│   ├── DeviceService.java
│   ├── HistoryService.java
│   └── MqttService.java
├── repository
├── entity
├── dto
│   ├── request
│   ├── response
│   └── event
├── config
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── MqttConfig.java
│   └── CorsConfig.java
└── exception
```

## 4. Trách nhiệm các lớp

### Controller

- `AuthController`: login, đọc profile theo username, cập nhật profile theo id.
- `SensorController`: latest, chart, lịch sử cảm biến có lọc và phân trang.
- `DeviceController`: danh sách LED, gửi lệnh, trạng thái MQTT Broker.
- `HistoryController`: tìm kiếm, lọc và phân trang lịch sử điều khiển.
- Controller nhận/trả DTO; không trả JPA entity trực tiếp.
- Request API đặt trong `dto/request`, response API đặt trong `dto/response`; payload nội bộ dùng cho MQTT/WebSocket đặt trong `dto/event`.

### Service

- `AuthService`: xác thực thông tin đăng nhập, cấp token và xử lý profile.
- `SensorService`: truy vấn dữ liệu mới nhất, chuỗi biểu đồ và lịch sử cảm biến.
- `DeviceService`: kiểm tra lệnh, tạo log, publish MQTT và xử lý timeout/ACK.
- `HistoryService`: áp dụng bộ lọc, sắp xếp và phân trang lịch sử.
- `MqttService`: kết nối broker, subscribe/publish, parse payload và chuyển sự kiện sang service.

Khi MQTT nhận dữ liệu cảm biến, parse payload, đổi ánh sáng thành giá trị số, lưu dữ liệu rồi broadcast DTO lên `/topic/sensor`. Khi nhận trạng thái LED, cập nhật thiết bị và log đang chờ, rồi broadcast DTO lên `/topic/device-status` và `/topic/action-log`. Không broadcast entity JPA thô để tránh lộ trường nội bộ và lỗi lazy loading.

### Repository và entity

Spring Data JPA quản lý các entity `User`, `Sensor`, `DataSensor`, `Device`, `ActionLog` và repository tương ứng. Dùng quan hệ entity thực sự cho khóa ngoại (ví dụ `ActionLog.device`, `ActionLog.user`), không gán FK bằng object rỗng.

Các truy vấn danh sách cần lọc nên hỗ trợ phân trang ở DB. Với truy vấn fetch join trong phân trang, khai báo count query riêng khi cần. Các hàm migration như `findBySensorIsNull()` hoặc `findByTempStatusIsNull()` chỉ thêm nếu xác nhận có dữ liệu cũ cần chuyển đổi; không cần thiết cho database mới.

## 5. Thiết kế dữ liệu MySQL

Các bảng dự kiến:

- `users`: username, BCrypt password hash, thông tin profile và URL liên kết.
- `sensors`: các loại temperature, humidity, light, unit và ngưỡng hợp lệ.
- `data_sensor`: giá trị, thời điểm đo, sensor FK và trạng thái/đánh giá nếu FE cần.
- `devices`: đúng ba mã `LED1`, `LED2`, `LED3`, trạng thái hiện tại và thời điểm cập nhật.
- `action_log`: thiết bị FK, user FK, command/action, trạng thái xử lý, thời gian gửi/ACK, lỗi.

Chốt kiểu thời gian và cách trả ISO 8601 theo timezone Việt Nam cùng DTO ở giai đoạn hợp đồng API. Tạo schema/migration MySQL và dữ liệu khởi tạo cho 3 LED, 3 loại cảm biến và tài khoản phát triển; không đưa mật khẩu thật vào repository.

## 6. Luồng điều khiển thiết bị

1. `POST /api/devices/control` nhận `DeviceControlRequest` gồm `command` và trường `user` tùy chọn để tương thích FE.
2. Chỉ chấp nhận lệnh đã định nghĩa, ví dụ `LED1_ON`, `LED2_OFF`, `ALL_ON`, `ALL_OFF`.
3. Lưu `ActionLog` trạng thái `LOADING` với quan hệ device/user hợp lệ.
4. Lệnh đơn ánh xạ sang `{ "deviceId": 1, "action": "on" }` trên topic firmware `device_control`.
5. Lệnh ALL tạo ba log LED. Firmware nhận `deviceId: 0` và trả ACK `deviceId: 0`; BE phải chốt cả ba log của cùng thao tác sau ACK này.
6. Khi nhận ACK ở `device_response`, cập nhật trạng thái thiết bị/log và broadcast DTO.
7. Nếu publish thất bại hoặc quá thời hạn chờ, chốt log `FAILED` và khôi phục trạng thái thiết bị đã xác nhận gần nhất.

Do firmware chưa gửi `command_id`, mỗi thiết bị chỉ cho phép một lệnh đang chờ tại một thời điểm. Ghép ACK dựa vào deviceId/action và log `LOADING` mới nhất. Nếu cần nhiều lệnh đồng thời hoặc retry an toàn, nâng cấp firmware để gửi command ID tương quan.

## 7. Realtime WebSocket

- STOMP endpoint `/ws`, hỗ trợ SockJS fallback nếu FE cần.
- Prefix gửi từ client: `/app`; broker prefix: `/topic`.
- `/topic/sensor`: dữ liệu cảm biến mới.
- `/topic/device-status`: trạng thái LED mới.
- `/topic/action-log`: kết quả lệnh và cập nhật lịch sử.
- Chỉ broadcast DTO ổn định, không broadcast entity.

## 8. Bảo mật và cấu hình

- Dùng BCrypt để lưu mật khẩu.
- `/api/auth/login` public; các REST API còn lại yêu cầu JWT.
- Không tin trường `user` do client gửi để quyết định người thao tác. Ưu tiên định danh từ principal/JWT; chỉ dùng HTTP Session nếu dự án quyết định chuyển sang xác thực session rõ ràng. Nếu body `user` vẫn được FE gửi, chỉ dùng để hiển thị hoặc đối chiếu, không dùng làm căn cứ phân quyền/ghi audit.
- WebSocket cần thống nhất cách truyền JWT khi handshake; đề xuất xác thực kết nối thay vì để topic lịch sử/thiết bị công khai.
- CORS chỉ cho phép origin FE được cấu hình. Không bật đồng thời wildcard origin và credentials trong cấu hình triển khai.
- MySQL, JWT và MQTT credentials lấy từ environment/config riêng; không hardcode bí mật.

## 9. Điểm cần đối chiếu trước khi tích hợp

| Đề xuất | Quyết định trong plan |
|---|---|
| Package `com.ptit.iot.*` và 4 controller | Áp dụng; đổi package skeleton hiện tại khi triển khai. |
| MQTT subscribe `iot/data_sensor`, `iot/devices_status` | Chưa áp dụng làm topic mặc định vì không khớp sketch. Dùng topic cấu hình được, mặc định theo sketch: `data_sensors`, `device_response`, publish `device_control`. Chỉ đổi nếu cập nhật firmware hoặc xác nhận broker bridge có alias. |
| Một sensor key `DHT11_LDR` | Không dùng nguyên key: sketch khai báo DHT22 + LDR và FE cần temperature/humidity/light riêng. Lưu/ánh xạ thành 3 sensor code tương ứng. |
| `@PrePersist` tự tính trạng thái | Chỉ dùng cho giá trị suy ra thuần từ entity. Nếu trạng thái phụ thuộc ngưỡng/cấu hình nghiệp vụ, tính trong service để dễ kiểm soát; không dùng callback để thay thế validation. |
| `findWithFilters(...)` với `LEFT JOIN FETCH` | Có thể áp dụng cho quan hệ many-to-one; phân trang phải có count query phù hợp. Migration finder chỉ cần nếu có dữ liệu legacy. |
| `sendControl(command)` overload tương thích cũ | Áp dụng nếu có caller cũ; overload mới nhận actor/DTO rõ ràng và gọi chung một luồng nghiệp vụ. |
| `anyRequest().permitAll()`, CSRF off, mọi CORS origin | Không áp dụng cho cấu hình cuối: trái yêu cầu authenticated. Dùng public login, bảo vệ REST còn lại bằng JWT, CORS theo origin FE. |
| User từ body, fallback HTTP Session | Không dùng làm nguồn tin cậy. Lấy actor từ principal; Session chỉ là lựa chọn nếu sau này thống nhất session auth thay JWT. |

## 10. Các phase implement tuần tự

Các phase đi theo thứ tự dưới đây. Hoàn thành đầu ra của phase hiện tại trước khi bắt đầu phase kế tiếp; không triển khai controller trước khi chốt DTO và mô hình dữ liệu mà controller phụ thuộc.

### Phase 0 — Chốt hợp đồng và quyết định tích hợp

**Công việc:**

- Đối chiếu FE để chốt JSON request/response, mã lỗi, phân trang, bộ lọc và định dạng thời gian cho 11 API.
- Chốt trường `UserData` trả về khi login, JWT claims/expiry và cách FE gửi token.
- Chốt `DeviceControlRequest`, ý nghĩa trường `user`, các giá trị `command`, status và ACK.
- Chốt DTO sensor (latest/chart/history), trạng thái cảm biến và WebSocket event payload.
- Ghi nhận MQTT topic/payload mặc định từ sketch và cấu hình có thể thay đổi.

**Đầu ra:** Hợp đồng DTO/API và danh sách quyết định đã chốt. Đây là đầu vào bắt buộc cho các phase sau.

### Phase 1 — Nền ứng dụng và cấu hình môi trường *(đã implement)*

**Công việc:**

- Đổi package gốc từ `com.example.demo` sang `com.ptit.iot`; sắp xếp class khởi động và package.
- Thêm dependency cần thiết cho REST, validation, JPA/MySQL, JWT/security, MQTT, WebSocket/STOMP.
- Khai báo cấu hình theo profile và environment variables cho MySQL, JWT, MQTT, CORS.
- Tạo exception handler và response/error format chung theo hợp đồng Phase 0.

**Đầu ra:** Ứng dụng có cấu trúc package/configuration nền, khởi động được với cấu hình development.

### Phase 2 — Schema MySQL, entity và repository *(đã implement)*

**Công việc:**

- Tạo migration/schema MySQL cho `users`, `sensors`, `data_sensor`, `devices`, `action_log`.
- Tạo entity, quan hệ khóa ngoại thực, repository và dữ liệu khởi tạo cho 3 LED/3 sensor.
- Tạo các truy vấn cần cho latest, chart, lọc/phân trang sensor và history.
- Chỉ thêm truy vấn migration legacy nếu xác nhận cần chuyển dữ liệu cũ.

**Đầu ra:** Mô hình lưu trữ MySQL sẵn sàng cho các service sử dụng.

### Phase 3 — Auth, JWT và Profile *(đã implement)*

**Công việc:**

- Cài BCrypt password encoder và xác thực username/password.
- Tạo login, phát JWT và cấu hình bảo vệ các REST API.
- Tạo lấy/cập nhật profile; actor đăng nhập được lấy từ principal, không tin request body làm danh tính.
- Thêm bootstrap user tùy chọn qua `BOOTSTRAP_USERNAME`, `BOOTSTRAP_PASSWORD`, `BOOTSTRAP_FULL_NAME`; không seed tài khoản/mật khẩu mặc định.
- Hợp đồng hiện tại: login nhận `{ "username", "password" }`, trả `accessToken`, `tokenType`, `expiresIn`, `userData`; profile DTO dùng camelCase và `studentCode` ánh xạ tới `users.student_id`.
- Chỉ cho phép người dùng đọc/cập nhật profile của chính mình; cần bổ sung role/admin nếu sau này cần quản trị user khác.
- JWT secret mặc định chỉ dành cho local development; production phải cấu hình `JWT_SECRET` riêng, tối thiểu 32 byte.

**API:** `POST /api/auth/login`, `GET /api/auth/profile/{username}`, `PUT /api/auth/profile/{id}`.

**Đầu ra:** Nhóm Auth/Profile hoạt động cùng cơ chế bảo vệ API thống nhất.

### Phase 4 — Sensor ingestion qua MQTT *(đã implement; cần cấu hình broker để chạy thực tế)*

**Công việc:**

- Khởi tạo Eclipse Paho client, reconnect và subscribe topic cảm biến theo sketch.
- Parse payload tiếng Việt, chuẩn hóa nhiệt độ/độ ẩm/ánh sáng; chuyển chuỗi lux thành số và xử lý payload sai.
- Lưu các bản ghi sensor vào MySQL, tính trạng thái theo ngưỡng đã chốt.
- Tạo DTO và broadcast reading mới lên `/topic/sensor`.
- Broker URL mặc định khớp sketch hiện tại (`tcp://10.179.205.227:2205`); có thể override bằng `MQTT_BROKER_URL`. Username/password bắt buộc đặt qua `MQTT_USERNAME`/`MQTT_PASSWORD` theo cấu hình broker, không hardcode trong source.

**Đầu ra:** Dữ liệu từ ESP32 đi được tới MySQL và có sự kiện realtime.

### Phase 5 — API truy vấn sensor

**Công việc:**

- Triển khai latest, chart theo `hours`, và lịch sử có lọc giá trị/thời gian cùng phân trang.
- Dùng repository truy vấn DB và DTO từ Phase 0; không trả entity trực tiếp.
- Thời gian trả về theo ISO 8601 với offset `+07:00`. `GET /api/sensor` nhận `page` (bắt đầu từ 1), `page_size` (1–100), `sensor_code`, `value_min`, `value_max`, `from`, `to`, `sort_by` (`time`, `value`, `id`) và `sort_dir` (`asc`, `desc`). Thời gian nhận ISO 8601 có offset hoặc local datetime; khoảng lọc dùng `from <= recordedAt < to`.
- `GET /api/sensor/chart` giới hạn `hours` từ 1 đến 168. `GET /api/sensor/latest` trả ba giá trị `temperature`, `humidity`, `light` và `lastUpdatedAt`; trường của loại cảm biến chưa có dữ liệu sẽ là `null`.
- Response lịch sử có `items`, `page`, `pageSize`, `totalItems`, `totalPages`; mỗi item chứa mã/tên sensor, giá trị, đơn vị, trạng thái và thời điểm đo.

**API:** `GET /api/sensor/latest`, `GET /api/sensor/chart`, `GET /api/sensor`.

**Đầu ra:** Các màn hình Dashboard/biểu đồ/Data Sensor có thể đọc dữ liệu đã lưu. Trạng thái: đã implement và compile.

### Phase 6 — MQTT thiết bị, điều khiển và ACK

**Công việc:**

- Tạo danh sách/trạng thái 3 LED và API trạng thái MQTT Broker.
- Parse lệnh đơn và ALL; tạo `ActionLog` với FK device/user trước khi publish.
- Publish topic/payload đúng sketch; nhận phản hồi, cập nhật trạng thái/log.
- Xử lý timeout, publish failure và giới hạn một lệnh pending trên mỗi LED do firmware chưa có `command_id`.
- Broadcast trạng thái device và `ActionLogDTO`; không broadcast JPA entity.
- `DeviceService.sendControl(command, actor)` chỉ nhận `LED1_ON/OFF`, `LED2_ON/OFF`, `LED3_ON/OFF`, `ALL_ON`, `ALL_OFF`. Actor lấy từ principal; overload không actor chỉ để tương thích caller nội bộ.
- Lưu log và đánh dấu thiết bị `LOADING` trong transaction đã commit trước khi publish QoS 0 theo payload firmware `{ "deviceId": 1, "action": "on" }`. `ALL_*` lưu ba log cùng `batchId`, rồi publish đúng một payload `deviceId: 0`.
- Subscribe `device_response`; ACK đơn được ghép theo LED + action, ACK `deviceId: 0` chốt cả batch. ACK success cập nhật trạng thái đã xác nhận; ACK fail/publish failure/timeout chốt log `FAILED` và phục hồi trạng thái xác nhận gần nhất.
- Để tránh ghép nhầm ACK do firmware chưa gửi command ID, khóa từng LED đang chờ; lệnh ALL chiếm cả ba LED cho tới khi ACK hoặc timeout. Timeout mặc định 10 giây, cấu hình qua `MQTT_COMMAND_TIMEOUT_MS`.

**Đầu ra:** Luồng điều khiển đơn/ALL, ACK, timeout và realtime DTO đã implement, compile. Chưa có REST controller cho điều khiển; sẽ nối ở Phase 7.

### Phase 7 — API Devices và History

**Công việc:**

- Hoàn thiện truy vấn danh sách thiết bị và tình trạng MQTT.
- Hoàn thiện tìm kiếm/lọc/phân trang lịch sử, bao gồm trạng thái loading/đã chốt.
- `GET /api/devices` trả danh sách DTO theo thứ tự ID với `deviceCode`, `name`, `status`, `lastConfirmedStatus` và `lastChangedAt`. `GET /api/devices/mqtt-status` trả `connected` cùng thời điểm kiểm tra.
- `POST /api/devices/control` nhận `{ "command": "LED1_ON", "user": "..." }`; `user` chỉ giữ tương thích payload FE và bị bỏ qua làm danh tính audit. Actor lấy từ JWT principal. Response gồm command chuẩn hóa và các log được tạo (một log lệnh đơn, ba log lệnh ALL).
- `GET /api/history` nhận `page`, `page_size`, `device` (một hoặc nhiều mã LED phân tách bằng dấu phẩy), `action`, `status`, `performed_by`/`username`, `from`, `to`, `sort_by`, `sort_dir`. Trả `PageResponse` gồm items và metadata trang. `from`/`to` nhận ISO 8601 có offset, local datetime hoặc ngày `yyyy-MM-dd`.
- Các DTO history trả trạng thái xử lý `LOADING`/`SUCCESS`/`FAILED`, `responseTimeSeconds`, `time` theo offset `+07:00` và thông tin người thao tác; các API được bảo vệ bằng JWT theo SecurityConfig.

**API:** `GET /api/devices`, `POST /api/devices/control`, `GET /api/devices/mqtt-status`, `GET /api/history`.

**Đầu ra:** REST cho điều khiển, trạng thái và lịch sử đã implement, compile và sẵn sàng để tích hợp FE. Chưa xác nhận runtime với broker MQTT.

### Phase 8 — WebSocket/STOMP và bảo vệ realtime *(đã implement)*

**Công việc:**

- Cấu hình `/ws`, SockJS fallback nếu FE cần, prefix `/app` và broker `/topic`.
- Hoàn thiện ba topic `/topic/sensor`, `/topic/device-status`, `/topic/action-log`.
- Thống nhất cách xác thực JWT khi handshake và kiểm soát quyền subscribe.

**Đầu ra:** Đã thêm STOMP inbound interceptor: CONNECT yêu cầu Bearer JWT hợp lệ, SUBSCRIBE chỉ cho phép ba topic đã định nghĩa, SEND từ client bị từ chối vì không có handler client-to-server. FE khởi tạo SockJS/STOMP sau đăng nhập và invalidate các truy vấn REST tương ứng khi nhận event. BE compile và FE production build thành công. Chưa kiểm tra kết nối realtime end-to-end với broker MQTT hoạt động.

### Phase 9 — Tích hợp FE và hoàn thiện *(đã implement)*

**Công việc:**

- Thay mock data/localStorage lần lượt ở Dashboard, Data Sensor, History và Profile bằng API.
- Nối WebSocket để cập nhật sensor, device status và action log; hiển thị loading/timeout/error.
- Chỉnh DTO/API nếu phát hiện chỗ FE và BE không khớp hợp đồng đã chốt.
- Cập nhật README cấu hình chạy local và quy trình khởi động MySQL/MQTT/BE/FE.

**Đầu ra:** FE đã chuyển Dashboard, Data Sensor, History, Profile sang API; thêm login/logout, lưu JWT trong sessionStorage, gửi Authorization Bearer, route guard, gọi điều khiển LED và tự tải lại dữ liệu khi có STOMP event. Cấu hình FE local ở `FE/.env.example`. Build FE (`pnpm build`) và compile BE (`mvnw -DskipTests compile`) thành công. Cần broker MQTT hoạt động và thông tin đăng nhập broker đúng để xác nhận luồng phần cứng/realtime thực tế.

## 11. Điều kiện hoàn tất

- Có đủ 11 endpoint hoạt động theo hợp đồng thống nhất; endpoint được bảo vệ đúng theo auth.
- Dữ liệu cảm biến từ MQTT được chuẩn hóa và lưu được vào MySQL; API sensor đọc đúng dữ liệu đã lưu.
- Lệnh LED đơn và ALL tạo lịch sử, publish đúng payload firmware, nhận ACK/timeout và cập nhật trạng thái.
- WebSocket phát đúng ba loại DTO realtime.
- FE dùng dữ liệu backend thay cho mock data ở các màn hình trong phạm vi.


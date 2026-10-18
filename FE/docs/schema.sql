-- =============================================================
-- Hệ thống giám sát nhiệt độ, bật tắt thiết bị phòng học
-- PostgreSQL 16 - DDL + dữ liệu khởi tạo
-- Xem giải thích chi tiết tại: docs/DATABASE – Thiết kế cơ sở dữ liệu.md
--
-- Cách chạy:
--   createdb -E UTF8 classroom_iot
--   psql -d classroom_iot -f docs/schema.sql
-- =============================================================

-- PostgreSQL không hỗ trợ CREATE DATABASE IF NOT EXISTS, cũng không có lệnh USE.
-- Tạo database bằng lệnh dưới (chạy ở database postgres), rồi kết nối vào nó
-- trước khi chạy phần còn lại của file này:
--   CREATE DATABASE classroom_iot ENCODING 'UTF8' TEMPLATE template0;
--   \c classroom_iot

-- Toàn bộ trường thời gian dùng TIMESTAMP (không kèm timezone) và lưu theo giờ
-- Việt Nam (UTC+7), giữ nguyên ngữ nghĩa của thiết kế gốc. Vì CURRENT_TIMESTAMP
-- và NOW() được quy đổi theo múi giờ của phiên làm việc, phải đặt múi giờ cho
-- database để giá trị mặc định luôn là giờ Việt Nam:
--   ALTER DATABASE classroom_iot SET timezone = 'Asia/Ho_Chi_Minh';
-- Nếu về sau cần hỗ trợ nhiều múi giờ thì đổi TIMESTAMP sang TIMESTAMPTZ.

DROP TABLE IF EXISTS device_history CASCADE;
DROP TABLE IF EXISTS sensor_data    CASCADE;
DROP TABLE IF EXISTS devices        CASCADE;
DROP TABLE IF EXISTS sensors        CASCADE;
DROP TABLE IF EXISTS users          CASCADE;

DROP TYPE IF EXISTS history_status_enum;
DROP TYPE IF EXISTS history_action_enum;
DROP TYPE IF EXISTS device_status_enum;
DROP TYPE IF EXISTS device_type_enum;

DROP FUNCTION IF EXISTS set_updated_at();

-- -------------------------------------------------------------
-- 0a. Kiểu ENUM (PostgreSQL không cho khai báo ENUM ngay tại cột như MySQL)
-- -------------------------------------------------------------
CREATE TYPE device_type_enum    AS ENUM ('FAN', 'LIGHT');
CREATE TYPE device_status_enum  AS ENUM ('ON', 'OFF', 'LOADING');
CREATE TYPE history_action_enum AS ENUM ('ON', 'OFF');
CREATE TYPE history_status_enum AS ENUM ('LOADING', 'ON', 'OFF', 'FAILED');

-- -------------------------------------------------------------
-- 0b. Hàm + trigger thay cho ON UPDATE CURRENT_TIMESTAMP của MySQL
-- -------------------------------------------------------------
CREATE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------------
-- 1. users - Thông tin người dùng
-- -------------------------------------------------------------
CREATE TABLE users (
  id            INTEGER      GENERATED ALWAYS AS IDENTITY,
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(100) NOT NULL,
  student_id    VARCHAR(20)  DEFAULT NULL,
  class_name    VARCHAR(50)  DEFAULT NULL,
  email         VARCHAR(100) DEFAULT NULL,
  avatar_url    VARCHAR(500) DEFAULT NULL,
  github_url    VARCHAR(500) DEFAULT NULL,
  figma_url     VARCHAR(500) DEFAULT NULL,
  postman_url   VARCHAR(500) DEFAULT NULL,
  report_url    VARCHAR(500) DEFAULT NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_users      PRIMARY KEY (id),
  CONSTRAINT uq_username   UNIQUE (username),
  CONSTRAINT uq_email      UNIQUE (email),
  CONSTRAINT uq_student_id UNIQUE (student_id)
);

-- MySQL dùng collation utf8mb4_unicode_ci nên username/email không phân biệt
-- hoa thường. PostgreSQL phân biệt hoa thường, nên chặn trùng bằng unique index
-- trên LOWER(). Khi đăng nhập phải so sánh: WHERE LOWER(username) = LOWER($1)
CREATE UNIQUE INDEX uq_username_lower ON users (LOWER(username));
CREATE UNIQUE INDEX uq_email_lower    ON users (LOWER(email));

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE users IS 'Thông tin tài khoản và hồ sơ người dùng (US01 Đăng nhập, US05 Profile)';

-- -------------------------------------------------------------
-- 2. sensors - Danh mục cảm biến
-- -------------------------------------------------------------
CREATE TABLE sensors (
  id          SMALLINT      GENERATED ALWAYS AS IDENTITY,
  sensor_code VARCHAR(20)   NOT NULL,
  sensor_name VARCHAR(100)  NOT NULL,
  unit        VARCHAR(10)   NOT NULL,
  min_value   NUMERIC(10,2) DEFAULT NULL,
  max_value   NUMERIC(10,2) DEFAULT NULL,
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_sensors     PRIMARY KEY (id),
  CONSTRAINT uq_sensor_code UNIQUE (sensor_code)
);

COMMENT ON TABLE sensors IS 'Danh mục cảm biến: temperature, light, humidity';

-- -------------------------------------------------------------
-- 3. sensor_data - Dữ liệu cảm biến
-- -------------------------------------------------------------
CREATE TABLE sensor_data (
  id          BIGINT        GENERATED ALWAYS AS IDENTITY,
  sensor_id   SMALLINT      NOT NULL,
  value       NUMERIC(10,2) NOT NULL,
  recorded_at TIMESTAMP     NOT NULL,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_sensor_data PRIMARY KEY (id),
  CONSTRAINT fk_sensor_data_sensor
    FOREIGN KEY (sensor_id) REFERENCES sensors (id)
    ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE INDEX idx_sensor_recorded ON sensor_data (sensor_id, recorded_at);
CREATE INDEX idx_recorded_at     ON sensor_data (recorded_at);
CREATE INDEX idx_sensor_value    ON sensor_data (sensor_id, value);

COMMENT ON TABLE sensor_data IS 'Bản ghi giá trị cảm biến do IoT Device gửi lên (US02 Dashboard, US03 Data Sensor)';

-- -------------------------------------------------------------
-- 4. devices - Danh mục thiết bị điều khiển
-- -------------------------------------------------------------
CREATE TABLE devices (
  id              SMALLINT           GENERATED ALWAYS AS IDENTITY,
  device_code     VARCHAR(30)        NOT NULL,
  device_name     VARCHAR(100)       NOT NULL,
  device_type     device_type_enum   NOT NULL,
  location        VARCHAR(100)       DEFAULT NULL,
  description     VARCHAR(255)       DEFAULT NULL,
  current_status  device_status_enum NOT NULL DEFAULT 'OFF',
  last_changed_at TIMESTAMP          DEFAULT NULL,
  created_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_devices     PRIMARY KEY (id),
  CONSTRAINT uq_device_code UNIQUE (device_code)
);

CREATE INDEX idx_device_type ON devices (device_type);

CREATE TRIGGER trg_devices_updated_at
  BEFORE UPDATE ON devices
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE devices IS 'Danh mục thiết bị điều khiển (quạt, đèn) và trạng thái hiện tại (US02 Dashboard)';

-- -------------------------------------------------------------
-- 5. device_history - Lịch sử bật/tắt thiết bị
-- -------------------------------------------------------------
CREATE TABLE device_history (
  id               BIGINT              GENERATED ALWAYS AS IDENTITY,
  device_id        SMALLINT            NOT NULL,
  user_id          INTEGER             DEFAULT NULL,
  action           history_action_enum NOT NULL,
  status           history_status_enum NOT NULL DEFAULT 'LOADING',
  requested_at     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  responded_at     TIMESTAMP           DEFAULT NULL,
  response_time_ms INTEGER             DEFAULT NULL,
  error_message    VARCHAR(255)        DEFAULT NULL,
  created_at       TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_device_history PRIMARY KEY (id),
  CONSTRAINT ck_response_time_ms
    CHECK (response_time_ms IS NULL OR response_time_ms >= 0),
  CONSTRAINT fk_history_device
    FOREIGN KEY (device_id) REFERENCES devices (id)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_history_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX idx_requested_at     ON device_history (requested_at);
CREATE INDEX idx_device_requested ON device_history (device_id, requested_at);
CREATE INDEX idx_status           ON device_history (status);
CREATE INDEX idx_action           ON device_history (action);
CREATE INDEX idx_user             ON device_history (user_id);

COMMENT ON TABLE device_history IS 'Lịch sử thao tác bật/tắt và phản hồi từ IoT Device (US02 Dashboard, US04 History)';

-- =============================================================
-- DỮ LIỆU KHỞI TẠO
-- =============================================================

-- Thay password_hash bằng bcrypt hash thật trước khi triển khai
INSERT INTO users
  (username, password_hash, full_name, student_id, class_name, email,
   avatar_url, github_url, figma_url, postman_url, report_url)
VALUES
  ('admin',
   '$2b$10$REPLACE_WITH_REAL_BCRYPT_HASH_OF_YOUR_PASSWORD',
   'Anh Tran', 'B21DCCN001', 'D21CQCN01-B',
   'anhtran@student.ptit.edu.vn',
   '/avatars/shadcn.jpg',
   'https://github.com/',
   'https://figma.com/',
   NULL,
   NULL);

INSERT INTO sensors (sensor_code, sensor_name, unit, min_value, max_value) VALUES
  ('temperature', 'Cảm biến nhiệt độ', '°C',  -10.00,     80.00),
  ('light',       'Cảm biến ánh sáng', 'lux',   0.00, 100000.00),
  ('humidity',    'Cảm biến độ ẩm',    '%',     0.00,    100.00);

INSERT INTO devices (device_code, device_name, device_type, location, description, current_status) VALUES
  ('fan-01',      'Quạt 1',       'FAN',   'Dãy trái',     'Làm mát khu vực dãy trái', 'OFF'),
  ('fan-02',      'Quạt 2',       'FAN',   'Dãy phải',     'Làm mát khu vực dãy phải', 'OFF'),
  ('light-board', 'Đèn bảng',     'LIGHT', 'Khu vực bảng', 'Chiếu sáng khu vực bảng',  'OFF'),
  ('light-left',  'Đèn dãy trái', 'LIGHT', 'Dãy trái',     'Chiếu sáng dãy bàn trái',  'OFF'),
  ('light-right', 'Đèn dãy phải', 'LIGHT', 'Dãy phải',     'Chiếu sáng dãy bàn phải',  'OFF');

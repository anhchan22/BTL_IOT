CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    student_id VARCHAR(20) NULL,
    class_name VARCHAR(50) NULL,
    email VARCHAR(100) NULL,
    avatar_url VARCHAR(500) NULL,
    github_url VARCHAR(500) NULL,
    figma_url VARCHAR(500) NULL,
    postman_url VARCHAR(500) NULL,
    report_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_student_id UNIQUE (student_id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sensors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sensor_key VARCHAR(30) NOT NULL,
    sensor_name VARCHAR(100) NOT NULL,
    unit VARCHAR(10) NOT NULL,
    min_value DECIMAL(10, 2) NULL,
    max_value DECIMAL(10, 2) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_sensors_sensor_key UNIQUE (sensor_key),
    CONSTRAINT ck_sensors_value_range CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE data_sensor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    sensor_id BIGINT NOT NULL,
    measured_value DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    recorded_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_data_sensor_sensor FOREIGN KEY (sensor_id) REFERENCES sensors (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_data_sensor_sensor_recorded (sensor_id, recorded_at, id),
    INDEX idx_data_sensor_recorded (recorded_at),
    INDEX idx_data_sensor_sensor_value (sensor_id, measured_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_code VARCHAR(30) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NULL,
    description VARCHAR(255) NULL,
    current_status VARCHAR(10) NOT NULL DEFAULT 'OFF',
    last_confirmed_status VARCHAR(10) NOT NULL DEFAULT 'OFF',
    last_changed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_devices_device_code UNIQUE (device_code),
    CONSTRAINT ck_devices_current_status CHECK (current_status IN ('ON', 'OFF', 'LOADING')),
    CONSTRAINT ck_devices_confirmed_status CHECK (last_confirmed_status IN ('ON', 'OFF')),
    INDEX idx_devices_status (current_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE action_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    action VARCHAR(10) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'LOADING',
    batch_id CHAR(36) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME NULL,
    response_time_ms INT NULL,
    error_message VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_action_log_device FOREIGN KEY (device_id) REFERENCES devices (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_action_log_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_action_log_action CHECK (action IN ('ON', 'OFF')),
    CONSTRAINT ck_action_log_status CHECK (status IN ('LOADING', 'SUCCESS', 'FAILED')),
    CONSTRAINT ck_action_log_response_time CHECK (response_time_ms IS NULL OR response_time_ms >= 0),
    INDEX idx_action_log_requested (requested_at, id),
    INDEX idx_action_log_device_requested (device_id, requested_at, id),
    INDEX idx_action_log_status (status),
    INDEX idx_action_log_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sensors (sensor_key, sensor_name, unit, min_value, max_value) VALUES
    ('temperature', 'Cảm biến nhiệt độ', '°C', -10.00, 80.00),
    ('humidity', 'Cảm biến độ ẩm', '%', 0.00, 100.00),
    ('light', 'Cảm biến ánh sáng', 'lux', 0.00, 100000.00);

INSERT INTO devices (device_code, device_name, location, description) VALUES
    ('LED1', 'LED1', 'Khu vực 1', 'Đèn LED điều khiển số 1'),
    ('LED2', 'LED2', 'Khu vực 2', 'Đèn LED điều khiển số 2'),
    ('LED3', 'LED3', 'Khu vực 3', 'Đèn LED điều khiển số 3');

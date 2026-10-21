ALTER TABLE devices
    ADD COLUMN alert_threshold_rain DECIMAL(10, 2) NULL,
    ADD CONSTRAINT ck_devices_alert_threshold_rain
        CHECK (alert_threshold_rain IS NULL OR (alert_threshold_rain >= 0.00 AND alert_threshold_rain <= 500.00));

INSERT INTO devices (device_code, device_name, location, description)
VALUES ('LED4', 'Đèn cảnh báo', 'Tự động', 'Tự bật khi độ mưa vượt ngưỡng đã cài đặt')
ON DUPLICATE KEY UPDATE device_code = VALUES(device_code);

ALTER TABLE devices
    ADD COLUMN alert_threshold_lux DECIMAL(10, 2) NULL,
    ADD CONSTRAINT ck_devices_alert_threshold
        CHECK (alert_threshold_lux IS NULL OR (alert_threshold_lux >= 0.00 AND alert_threshold_lux <= 100000.00));

INSERT INTO devices (
    id, device_code, device_name, location, description, current_status,
    last_confirmed_status, is_online, last_changed_at, created_at, updated_at,
    alert_threshold_lux
)
SELECT
    4,
    'LED4',
    'Đèn cảnh báo',
    'Tự động',
    'Tự bật khi ánh sáng vượt ngưỡng đã cài đặt',
    IF(EXISTS (SELECT 1 FROM light_alert_events WHERE ended_at IS NULL), 'ON', 'OFF'),
    IF(EXISTS (SELECT 1 FROM light_alert_events WHERE ended_at IS NULL), 'ON', 'OFF'),
    FALSE,
    COALESCE((SELECT MAX(COALESCE(ended_at, started_at)) FROM light_alert_events), CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    IF(settings.is_enabled, settings.threshold_lux, NULL)
FROM light_alert_settings settings
WHERE settings.id = 1;

INSERT INTO action_log (device_id, action, status, requested_at, responded_at, response_time_ms)
SELECT 4, 'ON', 'SUCCESS', started_at, started_at, 0
FROM light_alert_events;

INSERT INTO action_log (device_id, action, status, requested_at, responded_at, response_time_ms)
SELECT 4, 'OFF', 'SUCCESS', ended_at, ended_at, 0
FROM light_alert_events
WHERE ended_at IS NOT NULL;

DROP TABLE light_alert_events;
DROP TABLE light_alert_settings;

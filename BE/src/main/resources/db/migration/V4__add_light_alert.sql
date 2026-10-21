CREATE TABLE light_alert_settings (
    id TINYINT NOT NULL,
    threshold_lux DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_light_alert_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_light_alert_threshold CHECK (threshold_lux >= 0.00 AND threshold_lux <= 100000.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO light_alert_settings (id, threshold_lux, is_enabled)
VALUES (1, 0.00, FALSE);

CREATE TABLE light_alert_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_light_alert_events_started (started_at, id),
    INDEX idx_light_alert_events_active (ended_at, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

package com.ptit.iot.repository;

import java.time.LocalDateTime;

public interface SensorRecordCutoff {
	LocalDateTime getRecordedAt();

	Long getId();
}

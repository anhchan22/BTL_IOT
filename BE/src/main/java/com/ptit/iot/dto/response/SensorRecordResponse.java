package com.ptit.iot.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.ptit.iot.entity.SensorReadingStatus;

public record SensorRecordResponse(Long id, String sensorCode, String sensorName, BigDecimal value,
		String unit, SensorReadingStatus status, OffsetDateTime recordedAt) { }

package com.ptit.iot.dto.event;

import java.math.BigDecimal;
import java.util.Map;

import com.ptit.iot.entity.SensorReadingStatus;

public record SensorSnapshotEvent(
		long id,
		BigDecimal temperature,
		BigDecimal humidity,
		BigDecimal light,
		String time,
		Map<String, SensorReadingStatus> statuses) {
}

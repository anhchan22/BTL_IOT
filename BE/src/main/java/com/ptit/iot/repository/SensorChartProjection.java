package com.ptit.iot.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SensorChartProjection(
		LocalDateTime recordedAt,
		BigDecimal temperature,
		BigDecimal humidity,
		BigDecimal light) {
}

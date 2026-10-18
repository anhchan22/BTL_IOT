package com.ptit.iot.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record SensorChartPoint(
		long id,
		BigDecimal temperature,
		BigDecimal humidity,
		BigDecimal light,
		OffsetDateTime time) {

	private static final ZoneId CLASSROOM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	public static SensorChartPoint from(LocalDateTime time, BigDecimal temperature,
			BigDecimal humidity, BigDecimal light) {
		long id = time.atZone(CLASSROOM_ZONE).toInstant().toEpochMilli();
		return new SensorChartPoint(id, temperature, humidity, light, time.atZone(CLASSROOM_ZONE).toOffsetDateTime());
	}
}

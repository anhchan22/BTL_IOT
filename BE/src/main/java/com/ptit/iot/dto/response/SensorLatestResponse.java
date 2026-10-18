package com.ptit.iot.dto.response;

import java.time.OffsetDateTime;

public record SensorLatestResponse(
		SensorValueResponse temperature,
		SensorValueResponse humidity,
		SensorValueResponse light,
		OffsetDateTime lastUpdatedAt) {
}

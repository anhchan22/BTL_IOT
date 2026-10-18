package com.ptit.iot.service;

import java.math.BigDecimal;

import com.ptit.iot.dto.event.SensorSnapshotEvent;

public interface SensorIngestionService {
	SensorSnapshotEvent recordSnapshot(BigDecimal temperature, BigDecimal humidity, BigDecimal light);
}

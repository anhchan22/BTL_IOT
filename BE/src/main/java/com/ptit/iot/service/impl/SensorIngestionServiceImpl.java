package com.ptit.iot.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ptit.iot.dto.event.SensorSnapshotEvent;
import com.ptit.iot.entity.DataSensor;
import com.ptit.iot.entity.Sensor;
import com.ptit.iot.entity.SensorReadingStatus;
import com.ptit.iot.exception.BusinessException;
import com.ptit.iot.exception.DomainCode;
import com.ptit.iot.repository.DataSensorRepository;
import com.ptit.iot.repository.SensorRepository;
import com.ptit.iot.service.SensorIngestionService;
import com.ptit.iot.service.RainAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SensorIngestionServiceImpl implements SensorIngestionService {
	private static final ZoneId CLASSROOM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private final SensorRepository sensorRepository;
	private final DataSensorRepository dataSensorRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final RainAlertService rainAlertService;

	@Override
	@Transactional
	public SensorSnapshotEvent recordSnapshot(BigDecimal temperature, BigDecimal humidity, BigDecimal light, BigDecimal rain) {
		LocalDateTime recordedAt = LocalDateTime.now(CLASSROOM_ZONE);
		Map<String, BigDecimal> values = new LinkedHashMap<>();
		values.put("temperature", normalize(temperature));
		values.put("humidity", normalize(humidity));
		values.put("light", normalize(light));
		values.put("rain", normalize(rain));
		Map<String, SensorReadingStatus> statuses = new LinkedHashMap<>();
		for (var reading : values.entrySet()) {
			Sensor sensor = sensorRepository.findBySensorKey(reading.getKey())
					.orElseThrow(() -> new BusinessException(DomainCode.SENSOR_NOT_CONFIGURED, reading.getKey()));
			DataSensor data = new DataSensor();
			data.setSensor(sensor);
			data.setValue(reading.getValue());
			data.setRecordedAt(recordedAt);
			DataSensor saved = dataSensorRepository.save(data);
			statuses.put(reading.getKey(), saved.getStatus());
		}
		long eventId = recordedAt.atZone(CLASSROOM_ZONE).toInstant().toEpochMilli();
		SensorSnapshotEvent event = new SensorSnapshotEvent(eventId, values.get("temperature"), values.get("humidity"),
				values.get("light"), values.get("rain"), recordedAt.atZone(CLASSROOM_ZONE).toOffsetDateTime().toString(), Map.copyOf(statuses));
		rainAlertService.recordRainReading(values.get("rain"), recordedAt);
		eventPublisher.publishEvent(event);
		return event;
	}
	private BigDecimal normalize(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}

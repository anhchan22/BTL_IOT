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

	@Override
	@Transactional
	public SensorSnapshotEvent recordSnapshot(BigDecimal temperature, BigDecimal humidity, BigDecimal light) {
		// Ghi ba số đo tại cùng một thời điểm để biểu đồ ghép chúng thành một điểm.
		LocalDateTime recordedAt = LocalDateTime.now(CLASSROOM_ZONE);
		Map<String, BigDecimal> values = new LinkedHashMap<>();
		values.put("temperature", normalize(temperature));
		values.put("humidity", normalize(humidity));
		values.put("light", normalize(light));
		Map<String, SensorReadingStatus> statuses = new LinkedHashMap<>();
		// Mỗi vòng lặp tạo và lưu một bản ghi riêng: một cho nhiệt độ, một cho độ ẩm, một cho ánh sáng.
		for (var reading : values.entrySet()) {
			Sensor sensor = sensorRepository.findBySensorKey(reading.getKey())
					.orElseThrow(() -> new BusinessException(DomainCode.SENSOR_NOT_CONFIGURED, reading.getKey()));
			// Tạo bản ghi số đo gồm cảm biến, giá trị và thời điểm ghi nhận.
			DataSensor data = new DataSensor();
			data.setSensor(sensor);
			data.setValue(reading.getValue());
			data.setRecordedAt(recordedAt);
			// Gửi bản ghi qua kho dữ liệu để lưu;
			DataSensor saved = dataSensorRepository.save(data);
			statuses.put(reading.getKey(), saved.getStatus());
		}
		long eventId = recordedAt.atZone(CLASSROOM_ZONE).toInstant().toEpochMilli();
		SensorSnapshotEvent event = new SensorSnapshotEvent(eventId, values.get("temperature"), values.get("humidity"),
				values.get("light"), recordedAt.atZone(CLASSROOM_ZONE).toOffsetDateTime().toString(), Map.copyOf(statuses));
		// Tạo sự kiện nội bộ. MqttService sẽ gửi sự kiện sau khi giao dịch hoàn tất.
		eventPublisher.publishEvent(event);
		return event;
	}
	// Làm tròn số đo đến hai chữ số thập phân trước khi lưu.
	private BigDecimal normalize(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}

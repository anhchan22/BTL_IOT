package com.ptit.iot.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import com.ptit.iot.dto.response.PageResponse;
import com.ptit.iot.dto.response.SensorChartPoint;
import com.ptit.iot.dto.response.SensorLatestResponse;
import com.ptit.iot.dto.response.SensorRecordResponse;
import com.ptit.iot.dto.response.SensorValueResponse;
import com.ptit.iot.entity.DataSensor;
import com.ptit.iot.exception.BusinessException;
import com.ptit.iot.exception.DomainCode;
import com.ptit.iot.mapper.IoTMapper;
import com.ptit.iot.repository.DataSensorRepository;
import com.ptit.iot.repository.SensorChartProjection;
import com.ptit.iot.repository.SensorRecordCutoff;
import com.ptit.iot.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SensorServiceImpl implements SensorService {
	private static final ZoneId CLASSROOM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private final DataSensorRepository dataSensorRepository;
	private final IoTMapper mapper;

	@Override
	public SensorLatestResponse getLatest() {
		Optional<DataSensor> temperature = latest("temperature");
		Optional<DataSensor> humidity = latest("humidity");
		Optional<DataSensor> light = latest("light");
		LocalDateTime lastUpdated = List.of(temperature, humidity, light).stream().flatMap(Optional::stream)
				.map(DataSensor::getRecordedAt).max(LocalDateTime::compareTo).orElse(null);
		return new SensorLatestResponse(toValue(temperature), toValue(humidity), toValue(light), toOffset(lastUpdated));
	}

	@Override
	public List<SensorChartPoint> getChartData(int hours) {
		if (hours < 1 || hours > 168) throw new BusinessException(DomainCode.INVALID_HOURS);
		return getChartDataForMinutes(hours * 60);
	}

	@Override
	public List<SensorChartPoint> getChartDataForMinutes(int minutes) {
		if (minutes < 1 || minutes > 10080) throw new BusinessException(DomainCode.INVALID_HOURS);
		LocalDateTime fromTime = LocalDateTime.now(CLASSROOM_ZONE).minusMinutes(minutes);
		return dataSensorRepository.findChartPointsSince(fromTime).stream().map(this::toChartPoint).toList();
	}

	@Override
	public PageResponse<SensorRecordResponse> getSensorData(int page, int pageSize, String sensorCode,
			BigDecimal minValue, BigDecimal maxValue, String from, String to, String sortBy, String sortDir) {
		validateRange(page, pageSize, minValue, maxValue);
		LocalDateTime fromTime = parseTime(from, "from");
		LocalDateTime toTime = parseTime(to, "to");
		if (fromTime != null && toTime != null && !fromTime.isBefore(toTime)) throw new BusinessException(DomainCode.INVALID_TIME_RANGE);
		var cutoff = dataSensorRepository.findRecentRecordsCutoff();
		LocalDateTime cutoffTime = cutoff.map(SensorRecordCutoff::getRecordedAt).orElse(null);
		Long cutoffId = cutoff.map(SensorRecordCutoff::getId).orElse(null);
		String sortProperty = switch (sortBy == null ? "time" : sortBy.toLowerCase()) {
			case "time", "recorded_at", "recordedat" -> "recordedAt";
			case "value" -> "value";
			case "unit" -> "sensor.unit";
			case "id" -> "id";
			default -> throw new BusinessException(DomainCode.INVALID_SORT, sortBy);
		};
		Sort.Direction direction = parseDirection(sortDir);
		Page<DataSensor> result = dataSensorRepository.findWithFilters(cutoffTime, cutoffId,
				normalizeSensorCode(sensorCode), minValue, maxValue,
				fromTime, toTime, PageRequest.of(page - 1, pageSize, Sort.by(direction, sortProperty)));
		return new PageResponse<>(result.getContent().stream().map(mapper::toSensorRecordResponse).toList(),
				page, pageSize, result.getTotalElements(), result.getTotalPages());
	}

	private void validateRange(int page, int pageSize, BigDecimal min, BigDecimal max) {
		if (page < 1) throw new BusinessException(DomainCode.INVALID_PAGE);
		if (pageSize < 1 || pageSize > 100) throw new BusinessException(DomainCode.INVALID_PAGE_SIZE);
		if (min != null && max != null && min.compareTo(max) > 0) throw new BusinessException(DomainCode.INVALID_VALUE_RANGE);
	}
	private Sort.Direction parseDirection(String raw) {
		try { return Sort.Direction.fromString(raw == null ? "desc" : raw); }
		catch (IllegalArgumentException exception) { throw new BusinessException(DomainCode.INVALID_SORT_DIRECTION); }
	}
	private Optional<DataSensor> latest(String sensorKey) {
		return dataSensorRepository.findTopBySensor_SensorKeyOrderByRecordedAtDescIdDesc(sensorKey);
	}
	private SensorValueResponse toValue(Optional<DataSensor> reading) {
		if (reading.isEmpty()) return null;
		DataSensor data = reading.get();
		return new SensorValueResponse(data.getValue(), data.getSensor().getUnit(), data.getStatus(), toOffset(data.getRecordedAt()));
	}
	private SensorChartPoint toChartPoint(SensorChartProjection point) {
		return mapper.toSensorChartPoint(point);
	}
	private OffsetDateTime toOffset(LocalDateTime value) {
		return value == null ? null : value.atZone(CLASSROOM_ZONE).toOffsetDateTime();
	}
	private LocalDateTime parseTime(String raw, String field) {
		if (raw == null || raw.isBlank()) return null;
		try { return OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.ofHours(7)).toLocalDateTime(); }
		catch (DateTimeParseException ignored) {
			try { return LocalDateTime.parse(raw); }
			catch (DateTimeParseException exception) { throw new BusinessException(DomainCode.INVALID_TIME_FORMAT, field); }
		}
	}
	private String normalizeSensorCode(String raw) {
		if (raw == null || raw.isBlank()) return null;
		return switch (raw.trim().toLowerCase()) {
			case "temperature", "temp" -> "temperature";
			case "humidity", "humid" -> "humidity";
			case "light", "lux" -> "light";
			default -> throw new BusinessException(DomainCode.INVALID_SENSOR_CODE);
		};
	}
}

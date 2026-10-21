package com.ptit.iot.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.ptit.iot.dto.event.RainAlertStateChangedEvent;
import com.ptit.iot.dto.request.RainAlertSettingsRequest;
import com.ptit.iot.dto.response.RainAlertStatusResponse;
import com.ptit.iot.entity.ActionLog;
import com.ptit.iot.entity.ActionLogStatus;
import com.ptit.iot.entity.DataSensor;
import com.ptit.iot.entity.Device;
import com.ptit.iot.entity.DeviceAction;
import com.ptit.iot.entity.DeviceStatus;
import com.ptit.iot.mapper.IoTMapper;
import com.ptit.iot.repository.ActionLogRepository;
import com.ptit.iot.repository.DataSensorRepository;
import com.ptit.iot.repository.DeviceRepository;
import com.ptit.iot.service.RainAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RainAlertServiceImpl implements RainAlertService {
	private static final String ALERT_DEVICE_CODE = "LED4";
	private static final ZoneId CLASSROOM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final DeviceRepository deviceRepository;
	private final ActionLogRepository actionLogRepository;
	private final DataSensorRepository dataSensorRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final IoTMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public RainAlertStatusResponse getStatus() {
		return toStatus(alertDevice());
	}

	@Override
	public RainAlertStatusResponse updateSettings(RainAlertSettingsRequest request) {
		Device device = deviceRepository.findByDeviceCodeForUpdate(ALERT_DEVICE_CODE)
				.orElseThrow(() -> new IllegalStateException("Rain alert LED is missing from the devices table"));
		device.setAlertThresholdRain(request.threshold());
		deviceRepository.save(device);

		Optional<DataSensor> latest = latestRain();
		Optional<ActionLog> change = latest.flatMap(reading -> evaluate(device, reading.getValue(), reading.getRecordedAt()));
		change.ifPresent(log -> publishChange(device, log));
		return toStatus(device);
	}

	@Override
	public void recordRainReading(BigDecimal rain, LocalDateTime recordedAt) {
		Device device = deviceRepository.findByDeviceCodeForUpdate(ALERT_DEVICE_CODE)
				.orElseThrow(() -> new IllegalStateException("Rain alert LED is missing from the devices table"));
		evaluate(device, rain, recordedAt).ifPresent(log -> publishChange(device, log));
	}

	private Optional<ActionLog> evaluate(Device device, BigDecimal rain, LocalDateTime recordedAt) {
		BigDecimal threshold = device.getAlertThresholdRain();
		if (threshold == null) return Optional.empty();

		boolean active = device.getCurrentStatus() == DeviceStatus.ON;
		if (!active && rain.compareTo(threshold) <= 0) return Optional.empty();
		if (active && rain.compareTo(threshold) > 0) return Optional.empty();

		DeviceStatus newStatus = active ? DeviceStatus.OFF : DeviceStatus.ON;
		DeviceAction action = active ? DeviceAction.OFF : DeviceAction.ON;
		device.setCurrentStatus(newStatus);
		device.setLastConfirmedStatus(newStatus);
		device.setLastChangedAt(recordedAt);
		deviceRepository.save(device);

		ActionLog log = new ActionLog();
		log.setDevice(device);
		log.setAction(action);
		log.setStatus(ActionLogStatus.SUCCESS);
		log.setRequestedAt(recordedAt);
		log.setRespondedAt(recordedAt);
		log.setResponseTimeMs(0);
		return Optional.of(actionLogRepository.save(log));
	}

	private void publishChange(Device device, ActionLog log) {
		eventPublisher.publishEvent(new RainAlertStateChangedEvent(
				toStatus(device), mapper.toActionLogDTO(log), mapper.toDeviceDTO(device)));
	}

	private RainAlertStatusResponse toStatus(Device device) {
		LocalDate today = LocalDate.now(CLASSROOM_ZONE);
		LocalDateTime startOfDay = today.atStartOfDay();
		LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
		boolean active = device.getCurrentStatus() == DeviceStatus.ON;
		Optional<ActionLog> latestAction = active
				? actionLogRepository.findTopByDevice_DeviceCodeOrderByRequestedAtDesc(ALERT_DEVICE_CODE)
				: Optional.empty();
		return new RainAlertStatusResponse(
				device.getAlertThresholdRain() == null ? ZERO : device.getAlertThresholdRain(),
				device.getAlertThresholdRain() != null,
				active,
				actionLogRepository.countByDevice_DeviceCodeAndActionAndStatusAndRequestedAtGreaterThanEqualAndRequestedAtLessThan(
						ALERT_DEVICE_CODE, DeviceAction.ON, ActionLogStatus.SUCCESS, startOfDay, startOfTomorrow),
				latestAction.filter(log -> log.getAction() == DeviceAction.ON)
						.map(log -> log.getRequestedAt().atZone(CLASSROOM_ZONE).toOffsetDateTime()).orElse(null));
	}

	private Device alertDevice() {
		return deviceRepository.findByDeviceCode(ALERT_DEVICE_CODE)
				.orElseThrow(() -> new IllegalStateException("Rain alert LED is missing from the devices table"));
	}

	private Optional<DataSensor> latestRain() {
		return dataSensorRepository.findTopBySensor_SensorKeyOrderByRecordedAtDescIdDesc("rain");
	}
}

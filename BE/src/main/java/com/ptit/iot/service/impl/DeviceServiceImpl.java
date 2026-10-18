package com.ptit.iot.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.ptit.iot.dto.response.ActionLogDTO;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.MqttStatusResponse;
import com.ptit.iot.entity.ActionLog;
import com.ptit.iot.entity.ActionLogStatus;
import com.ptit.iot.entity.Device;
import com.ptit.iot.entity.DeviceAction;
import com.ptit.iot.entity.DeviceStatus;
import com.ptit.iot.entity.User;
import com.ptit.iot.exception.BusinessException;
import com.ptit.iot.exception.DomainCode;
import com.ptit.iot.mapper.IoTMapper;
import com.ptit.iot.repository.ActionLogRepository;
import com.ptit.iot.repository.DeviceRepository;
import com.ptit.iot.repository.UserRepository;
import com.ptit.iot.security.AuthenticatedUser;
import com.ptit.iot.service.DeviceService;
import com.ptit.iot.service.MqttService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
	private static final List<String> DEVICE_CODES = List.of("LED1", "LED2", "LED3");
	private static final ZoneId CLASSROOM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private final DeviceRepository deviceRepository;
	private final ActionLogRepository actionLogRepository;
	private final UserRepository userRepository;
	private final MqttService mqttService;
	private final SimpMessagingTemplate messagingTemplate;
	private final IoTMapper mapper;
	private final PlatformTransactionManager transactionManager;
	@Value("${app.mqtt.command-timeout-ms:10000}")
	private long commandTimeoutMs;

	@Override
	public List<ActionLogDTO> sendControl(String command, AuthenticatedUser actor) {
		ParsedCommand parsed = parseCommand(command);
		CommandMutation created = transactionTemplate().execute(status -> createPending(parsed, actor));
		if (created == null) throw new BusinessException(DomainCode.INTERNAL_ERROR);
		broadcast(created);
		try {
			mqttService.publishDeviceControl(parsed.deviceId(), parsed.action());
			return created.logs();
		} catch (RuntimeException exception) {
			CommandMutation failed = transactionTemplate().execute(status -> failByLogIds(
					created.logs().stream().map(ActionLogDTO::id).toList(), "MQTT publish failed"));
			if (failed != null) broadcast(failed);
			return failed == null ? created.logs() : failed.logs();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<DeviceDTO> getDevices() {
		return deviceRepository.findAllByOrderByIdAsc().stream().map(mapper::toDeviceDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public MqttStatusResponse getMqttStatus() {
		return new MqttStatusResponse(mqttService.isConnected(), OffsetDateTime.now(CLASSROOM_ZONE));
	}

	@Override
	public void handleDeviceAck(int deviceId, String actionText, String resultText) {
		DeviceAction action = parseAction(actionText);
		if (action == null || resultText == null) return;
		String result = resultText.trim().toLowerCase(Locale.ROOT);
		if (!result.equals("success") && !result.equals("fail")) return;
		CommandMutation mutation = transactionTemplate().execute(status -> {
			List<String> codes = deviceId == 0 ? DEVICE_CODES : deviceCodesForId(deviceId);
			if (codes.isEmpty()) return null;
			List<Device> locked = deviceRepository.findByDeviceCodesForUpdate(codes);
			List<ActionLog> pending;
			if (deviceId == 0) {
				ActionLog candidate = actionLogRepository.findAllByStatusAndBatchIdIsNotNull(ActionLogStatus.LOADING).stream()
						.filter(log -> log.getAction() == action)
						.max(Comparator.comparing(ActionLog::getRequestedAt)).orElse(null);
				if (candidate == null) return null;
				pending = actionLogRepository.findByBatchIdAndStatus(candidate.getBatchId(), ActionLogStatus.LOADING);
				if (pending.size() != DEVICE_CODES.size() || pending.stream().anyMatch(log -> log.getAction() != action)) return null;
			} else {
				pending = actionLogRepository.findTopByDevice_DeviceCodeAndStatusAndActionOrderByRequestedAtDesc(
						codes.getFirst(), ActionLogStatus.LOADING, action).map(List::of).orElse(null);
				if (pending == null) return null;
			}
			return completeLogs(pending, locked, result.equals("success"), result.equals("success") ? null : "Device rejected command");
		});
		if (mutation != null) broadcast(mutation);
	}

	@Override
	public void expireTimedOutCommands() {
		CommandMutation mutation = transactionTemplate().execute(status -> {
			LocalDateTime cutoff = LocalDateTime.now(CLASSROOM_ZONE).minusNanos(commandTimeoutMs * 1_000_000L);
			List<ActionLog> stale = actionLogRepository.findAllByStatusAndRequestedAtBefore(ActionLogStatus.LOADING, cutoff);
			if (stale.isEmpty()) return null;
			Map<String, List<ActionLog>> groups = new LinkedHashMap<>();
			for (ActionLog log : stale) {
				String groupKey = log.getBatchId() == null ? "log:" + log.getId() : "batch:" + log.getBatchId();
				groups.computeIfAbsent(groupKey, ignored -> new ArrayList<>()).add(log);
			}
			List<ActionLogDTO> logs = new ArrayList<>();
			Map<String, DeviceDTO> deviceStatuses = new LinkedHashMap<>();
			for (List<ActionLog> group : groups.values()) {
				List<String> codes = group.stream().map(log -> log.getDevice().getDeviceCode()).distinct().sorted().toList();
				List<Device> locked = deviceRepository.findByDeviceCodesForUpdate(codes);
				CommandMutation failed = failByLogIds(group.stream().map(ActionLog::getId).toList(), "Device response timed out", locked);
				if (failed != null) {
					logs.addAll(failed.logs());
					failed.devices().forEach(device -> deviceStatuses.put(device.deviceCode(), device));
				}
			}
			return logs.isEmpty() ? null : new CommandMutation(logs, List.copyOf(deviceStatuses.values()));
		});
		if (mutation != null) broadcast(mutation);
	}

	private CommandMutation createPending(ParsedCommand command, AuthenticatedUser actor) {
		List<Device> devices = command.deviceId() == 0
				? deviceRepository.findByDeviceCodesForUpdate(DEVICE_CODES)
				: deviceRepository.findByDeviceCodeForUpdate("LED" + command.deviceId()).stream().toList();
		if (devices.size() != (command.deviceId() == 0 ? 3 : 1)) throw new BusinessException(DomainCode.DEVICE_NOT_CONFIGURED);
		if (devices.stream().anyMatch(device -> device.getCurrentStatus() == DeviceStatus.LOADING)) throw new BusinessException(DomainCode.DEVICE_BUSY);
		User user = actor == null ? null : userRepository.findById(actor.id())
				.orElseThrow(() -> new BusinessException(DomainCode.USER_NOT_FOUND));
		String batchId = command.deviceId() == 0 ? UUID.randomUUID().toString() : null;
		LocalDateTime requestedAt = LocalDateTime.now(CLASSROOM_ZONE);
		List<ActionLog> logs = new ArrayList<>();
		for (Device device : devices) {
			device.setCurrentStatus(DeviceStatus.LOADING);
			ActionLog log = new ActionLog();
			log.setDevice(device);
			log.setUser(user);
			log.setAction(command.action());
			log.setStatus(ActionLogStatus.LOADING);
			log.setBatchId(batchId);
			log.setRequestedAt(requestedAt);
			logs.add(actionLogRepository.save(log));
		}
		return new CommandMutation(toLogDtos(logs), toDeviceDtos(devices));
	}

	private CommandMutation failByLogIds(List<Long> ids, String message) { return failByLogIds(ids, message, null); }
	private CommandMutation failByLogIds(List<Long> ids, String message, List<Device> alreadyLocked) {
		if (ids.isEmpty()) return null;
		List<ActionLog> logs = actionLogRepository.findAllById(ids).stream().filter(log -> log.getStatus() == ActionLogStatus.LOADING).toList();
		if (logs.isEmpty()) return null;
		List<String> codes = logs.stream().map(log -> log.getDevice().getDeviceCode()).distinct().sorted().toList();
		List<Device> devices = alreadyLocked == null ? deviceRepository.findByDeviceCodesForUpdate(codes) : alreadyLocked;
		return completeLogs(logs, devices, false, message);
	}
	private CommandMutation completeLogs(List<ActionLog> logs, List<Device> devices, boolean success, String error) {
		LocalDateTime respondedAt = LocalDateTime.now(CLASSROOM_ZONE);
		for (ActionLog log : logs) {
			log.setStatus(success ? ActionLogStatus.SUCCESS : ActionLogStatus.FAILED);
			log.setRespondedAt(respondedAt);
			log.setResponseTimeMs((int) Math.max(0, Duration.between(log.getRequestedAt(), respondedAt).toMillis()));
			log.setErrorMessage(error);
		}
		for (Device device : devices) {
			DeviceAction action = logs.stream().filter(log -> log.getDevice().getId().equals(device.getId())).map(ActionLog::getAction).findFirst().orElse(null);
			if (success && action != null) {
				DeviceStatus newStatus = action == DeviceAction.ON ? DeviceStatus.ON : DeviceStatus.OFF;
				device.setCurrentStatus(newStatus);
				device.setLastConfirmedStatus(newStatus);
				device.setLastChangedAt(respondedAt);
			} else device.setCurrentStatus(device.getLastConfirmedStatus());
		}
		return new CommandMutation(toLogDtos(logs), toDeviceDtos(devices));
	}
	private ParsedCommand parseCommand(String command) {
		String normalized = command == null ? "" : command.trim().toUpperCase(Locale.ROOT);
		String[] parts = normalized.split("_");
		if (parts.length != 2) throw new BusinessException(DomainCode.INVALID_DEVICE_COMMAND, normalized);
		DeviceAction action;
		try { action = DeviceAction.valueOf(parts[1]); }
		catch (IllegalArgumentException exception) { throw new BusinessException(DomainCode.INVALID_DEVICE_COMMAND, normalized); }
		if (parts[0].equals("ALL")) return new ParsedCommand(0, action);
		if (parts[0].matches("LED[1-3]")) return new ParsedCommand(parts[0].charAt(3) - '0', action);
		throw new BusinessException(DomainCode.INVALID_DEVICE_COMMAND, normalized);
	}
	private DeviceAction parseAction(String action) {
		if (action == null) return null;
		try { return DeviceAction.valueOf(action.trim().toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException exception) { return null; }
	}
	private List<String> deviceCodesForId(int id) { return id >= 1 && id <= 3 ? List.of("LED" + id) : List.of(); }
	private List<ActionLogDTO> toLogDtos(List<ActionLog> logs) { return logs.stream().map(mapper::toActionLogDTO).toList(); }
	private List<DeviceDTO> toDeviceDtos(List<Device> devices) { return devices.stream().map(mapper::toDeviceDTO).toList(); }
	private void broadcast(CommandMutation mutation) {
		mutation.devices().forEach(device -> messagingTemplate.convertAndSend("/topic/device-status", device));
		mutation.logs().forEach(log -> messagingTemplate.convertAndSend("/topic/action-log", log));
	}
	private TransactionTemplate transactionTemplate() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template;
	}
	private record ParsedCommand(int deviceId, DeviceAction action) { }
	private record CommandMutation(List<ActionLogDTO> logs, List<DeviceDTO> devices) { }
}

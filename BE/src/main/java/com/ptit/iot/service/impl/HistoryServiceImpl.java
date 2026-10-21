package com.ptit.iot.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ptit.iot.dto.response.HistoryRecordDTO;
import com.ptit.iot.dto.response.PageResponse;
import com.ptit.iot.entity.ActionLog;
import com.ptit.iot.entity.ActionLogStatus;
import com.ptit.iot.entity.DeviceAction;
import com.ptit.iot.exception.BusinessException;
import com.ptit.iot.exception.DomainCode;
import com.ptit.iot.mapper.IoTMapper;
import com.ptit.iot.repository.ActionLogRepository;
import com.ptit.iot.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryServiceImpl implements HistoryService {
	private static final List<String> DEVICE_CODES = List.of("LED1", "LED2", "LED3", "LED4");
	private final ActionLogRepository actionLogRepository;
	private final IoTMapper mapper;

	@Override
	public PageResponse<HistoryRecordDTO> search(int page, int pageSize, String deviceFilter, String actionFilter,
			String statusFilter, String performedBy, String from, String to, String sortBy, String sortDir) {
		if (page < 1) throw new BusinessException(DomainCode.INVALID_PAGE);
		if (pageSize < 1 || pageSize > 100) throw new BusinessException(DomainCode.INVALID_PAGE_SIZE);
		List<String> deviceCodes = parseDeviceCodes(deviceFilter);
		DeviceAction action = parseAction(actionFilter);
		ActionLogStatus status = parseStatus(statusFilter);
		LocalDateTime fromTime = parseTime(from, "from", false);
		LocalDateTime toTime = parseTime(to, "to", true);
		if (fromTime != null && toTime != null && !fromTime.isBefore(toTime)) throw new BusinessException(DomainCode.INVALID_TIME_RANGE);
		String property = switch (sortBy == null ? "time" : sortBy.toLowerCase(Locale.ROOT)) {
			case "time", "requested_at", "requestedat" -> "requestedAt";
			case "id" -> "id";
			case "device", "device_code" -> "device.deviceCode";
			case "performed_by", "user" -> "user.username";
			case "action" -> "action";
			case "status" -> "status";
			case "response_time", "response_time_ms" -> "responseTimeMs";
			default -> throw new BusinessException(DomainCode.INVALID_SORT, sortBy);
		};
		Sort.Direction direction;
		try { direction = Sort.Direction.fromString(sortDir == null ? "desc" : sortDir); }
		catch (IllegalArgumentException exception) { throw new BusinessException(DomainCode.INVALID_SORT_DIRECTION); }
		String username = performedBy == null || performedBy.isBlank() ? null : performedBy.trim();
		var pageable = PageRequest.of(page - 1, pageSize, Sort.by(direction, property));
		Page<ActionLog> result;
		if (deviceCodes == null || deviceCodes.isEmpty()) {
			result = actionLogRepository.findWithFilters(null, action, status, fromTime, toTime, username, pageable);
		} else if (deviceCodes.size() == 1) {
			result = actionLogRepository.findWithFilters(deviceCodes.getFirst(), action, status, fromTime, toTime, username, pageable);
		} else {
			result = actionLogRepository.findWithDeviceCodes(deviceCodes, action, status, fromTime, toTime, username, pageable);
		}
		return new PageResponse<>(result.getContent().stream().map(mapper::toHistoryRecordDTO).toList(),
				page, pageSize, result.getTotalElements(), result.getTotalPages());
	}

	private List<String> parseDeviceCodes(String raw) {
		if (raw == null || raw.isBlank()) return null;
		List<String> codes = Arrays.stream(raw.split(",")).map(String::trim).filter(code -> !code.isEmpty())
				.map(code -> code.toUpperCase(Locale.ROOT)).distinct().toList();
		if (codes.isEmpty() || codes.stream().anyMatch(code -> !DEVICE_CODES.contains(code))) throw new BusinessException(DomainCode.INVALID_DEVICE_CODE);
		return codes;
	}
	private DeviceAction parseAction(String raw) {
		if (raw == null || raw.isBlank()) return null;
		try { return DeviceAction.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException exception) { throw new BusinessException(DomainCode.INVALID_ACTION); }
	}
	private ActionLogStatus parseStatus(String raw) {
		if (raw == null || raw.isBlank()) return null;
		try { return ActionLogStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException exception) { throw new BusinessException(DomainCode.INVALID_STATUS); }
	}
	private LocalDateTime parseTime(String raw, String field, boolean upperBound) {
		if (raw == null || raw.isBlank()) return null;
		try { return OffsetDateTime.parse(raw.trim()).withOffsetSameInstant(ZoneOffset.ofHours(7)).toLocalDateTime(); }
		catch (DateTimeParseException ignored) {
			try { return LocalDateTime.parse(raw.trim()); }
			catch (DateTimeParseException alsoIgnored) {
				try { LocalDate date = LocalDate.parse(raw.trim()); return upperBound ? date.plusDays(1).atStartOfDay() : date.atStartOfDay(); }
				catch (DateTimeParseException exception) { throw new BusinessException(DomainCode.INVALID_TIME_FORMAT, field); }
			}
		}
	}
}

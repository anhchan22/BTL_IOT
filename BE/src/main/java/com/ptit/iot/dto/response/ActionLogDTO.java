package com.ptit.iot.dto.response;

import java.time.OffsetDateTime;
import com.ptit.iot.entity.ActionLogStatus;
import com.ptit.iot.entity.DeviceAction;

public record ActionLogDTO(Long id, String deviceCode, DeviceAction action, ActionLogStatus status,
		String username, String batchId, OffsetDateTime requestedAt, OffsetDateTime respondedAt,
		Integer responseTimeMs, String errorMessage) { }

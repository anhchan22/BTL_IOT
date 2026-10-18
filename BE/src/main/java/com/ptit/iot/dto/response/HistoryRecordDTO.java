package com.ptit.iot.dto.response;

import java.time.OffsetDateTime;
import com.ptit.iot.entity.ActionLogStatus;
import com.ptit.iot.entity.DeviceAction;

public record HistoryRecordDTO(Long id, String device, DeviceAction action, String performedBy,
		Double responseTimeSeconds, ActionLogStatus status, OffsetDateTime time, Integer responseTimeMs,
		OffsetDateTime respondedAt, String errorMessage, String batchId) { }

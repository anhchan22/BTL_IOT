package com.ptit.iot.dto.event;

import java.util.List;

import com.ptit.iot.entity.DeviceAction;

public record DeviceControlEvent(
		int deviceId,
		DeviceAction action,
		String batchId,
		List<Long> actionLogIds) {
}

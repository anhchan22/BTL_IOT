package com.ptit.iot.dto.response;

import java.time.OffsetDateTime;
import com.ptit.iot.entity.DeviceStatus;

public record DeviceDTO(Long id, String deviceCode, String name, String location, String description,
		DeviceStatus status, DeviceStatus lastConfirmedStatus, OffsetDateTime lastChangedAt) { }

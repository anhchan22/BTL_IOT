package com.ptit.iot.dto.response;

import java.time.OffsetDateTime;
import com.ptit.iot.entity.DeviceStatus;

public record DeviceStatusDTO(String deviceCode, DeviceStatus status, OffsetDateTime lastChangedAt) { }
